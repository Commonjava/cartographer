/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.INTERNAL.ops;

import org.apache.commons.io.IOUtils;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.GraphResolver;
import org.commonjava.cartographer.graph.RecipeResolver;
import org.commonjava.cartographer.graph.RepoContentCollector;
import org.commonjava.cartographer.graph.agg.ProjectRefCollection;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.cartographer.spi.graph.discover.ProjectRelationshipDiscoverer;
import org.commonjava.cartographer.graph.fn.MultiGraphFunction;
import org.commonjava.cartographer.ops.ResolveOps;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.commonjava.cartographer.INTERNAL.graph.agg.AggregationUtils.collectProjectVersionReferences;

public class ResolveOpsImpl
                implements ResolveOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphResolver resolver;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    private ArtifactManager artifacts;

    @Inject
    protected MavenPomReader pomReader;

    @Inject
    private RecipeResolver recipeResolver;

    @Inject
    @ExecutorConfig( daemon = true, named = "carto-graph-ops", priority = 9, threads = 16 )
    private ExecutorService executor;

    protected ResolveOpsImpl()
    {
    }

    public ResolveOpsImpl( final DiscoverySourceManager sourceManager, final ProjectRelationshipDiscoverer discoverer,
                           final ArtifactManager artifacts, final ExecutorService executor,
                           final RecipeResolver dtoResolver, GraphResolver resolver )
    {
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.artifacts = artifacts;
        this.executor = executor;
        this.recipeResolver = dtoResolver;
        this.resolver = resolver;
    }

    @Override
    public Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveRepositoryContents(
                    final RepositoryContentRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        if ( recipe == null || !recipe.isValid() )
        {
            throw new CartoDataException( "Repository content request is invalid: {}", recipe );
        }

        final URI sourceUri = sourceManager.createSourceURI( recipe.getSourceLocation().getUri() );
        if ( sourceUri == null )
        {
            throw new CartoDataException( "Invalid source format: '{}'. Use the form: '{}' instead.",
                                          recipe.getSourceLocation(), sourceManager.getFormatHint() );
        }

        final Map<ProjectVersionRef, ProjectRefCollection> refMap = resolveReferenceMap( recipe );
        final List<RepoContentCollector> collectors = collectContent( refMap, recipe );

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> itemMap = new HashMap<>();
        for ( final RepoContentCollector collector : collectors )
        {
            final Map<ArtifactRef, ConcreteResource> items = collector.getItems();

            if ( items != null && !items.isEmpty() )
            {
                logger.debug( "{} Returning for: {}\n\n  {}", collector, collector.getRef(),
                              new JoinString( "\n  ", items.entrySet() ) );
                Map<ArtifactRef, ConcreteResource> existingItems = itemMap.get( collector.getRef() );
                if ( existingItems == null )
                {
                    itemMap.put( collector.getRef(), items );
                    existingItems = items;
                }
                else
                {
                    existingItems.putAll( items );
                }

                logger.debug( "{} Accumulated for: {}\n\n  {}", collector, collector.getRef(),
                              new JoinString( "\n  ", existingItems.entrySet() ) );
            }
            else
            {
                logger.warn( "{} No items returned for: {}", collector, collector.getRef() );
            }
        }

        return itemMap;
    }

    private List<RepoContentCollector> collectContent( final Map<ProjectVersionRef, ProjectRefCollection> refMap,
                                                       final RepositoryContentRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final Location location = recipe.getSourceLocation();
        final Set<Location> excluded = recipe.getExcludedSourceLocations();

        if ( excluded != null && excluded.contains( location ) )
        {
            // no sense in going through all the rest if everything is excluded...
            throw new CartoDataException(
                            "RepositoryContentRequest is insane! Source location is among those excluded!" );
        }

        int projectCounter = 1;
        final int projectSz = refMap.size();
        final List<RepoContentCollector> collectors = new ArrayList<>( projectSz );

        final DiscoveryConfig dconf = recipe.getDiscoveryConfig();

        for ( final Map.Entry<ProjectVersionRef, ProjectRefCollection> entry : refMap.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final ProjectRefCollection refs = entry.getValue();

            final RepoContentCollector collector =
                            new RepoContentCollector( ref, refs, recipe, location, dconf, artifacts, discoverer,
                                                      excluded, projectCounter, projectSz );

            collectors.add( collector );

            projectCounter++;
        }

        final CountDownLatch latch = new CountDownLatch( collectors.size() );
        for ( final RepoContentCollector collector : collectors )
        {
            collector.setLatch( latch );
            executor.execute( collector );
        }

        // TODO: timeout with loop...
        while ( latch.getCount() > 0 )
        {
            logger.info( "Waiting for {} more content-collection threads to complete.", latch.getCount() );
            try
            {
                latch.await( 2, TimeUnit.SECONDS );
            }
            catch ( final InterruptedException e )
            {
                logger.error( "Abandoning repo-content assembly for: {}", recipe );
            }
        }

        return collectors;
    }

    /**
     * Discover the dependency graphs for the configured graph composition, and then traverse them to construct a mapping of GAV to set of references
     * that can be used to render various kinds of output. If the request contains injectedBOMs, then read the managed dependencies from these into
     * a mapping of GA -> GAV that we can pass into the {@link ViewParams} we'll eventually use to discover and traverse the graph.
     * <br/>
     * Returns null if {@link RepositoryContentRequest#setSourceLocation(Location)} hasn't
     * been called before this method is called.
     * <br/>
     * @throws {@link CartoDataException} if one or more of the request's injected BOMs cannot be resolved or if an
     * unexpected problem takes place during graph resolution or traversal.
     * @throws {@link CartoRequestException} if the request doesn't contain enough basic
     * info to be used (See: {@link RepositoryContentRequest#isValid()}) or if the source {@link Location} hasn't been set on the request
     */
    private Map<ProjectVersionRef, ProjectRefCollection> resolveReferenceMap( final RepositoryContentRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        logger.info( "Building repository for: {}", recipe );

        recipeResolver.resolve( recipe );

        final Map<ProjectVersionRef, ProjectRefCollection> refMap = new HashMap<>();
        final MultiGraphFunction<Set<ProjectRelationship<?>>> extractor = ( allRels, graphMap ) -> {
            try
            {
                refMap.putAll( collectProjectVersionReferences( allRels ) );

                for ( final RelationshipGraph graph : graphMap.values() )
                {
                    for ( final ProjectVersionRef root : graph.getRoots() )
                    {
                        ProjectRefCollection refCollection = refMap.get( root );
                        if ( refCollection == null )
                        {
                            refCollection = new ProjectRefCollection();
                            refCollection.addVersionRef( root );

                            refMap.put( root, refCollection );
                        }

                        if ( root instanceof ArtifactRef )
                        {
                            refCollection.addArtifactRef( (ArtifactRef) root );
                        }
                    }
                }
            }
            finally
            {
                graphMap.values().forEach( IOUtils::closeQuietly );
            }
        };

        resolver.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, recipe, ( allRefs, allRels, roots ) -> allRels.get(),
                                              extractor );

        return refMap;
    }

}
