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
package org.commonjava.maven.cartographer.ops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.result.MetadataCollationResult;
import org.commonjava.maven.cartographer.ops.fn.MatchingProjectFunction;
import org.commonjava.maven.cartographer.ops.fn.ProjectCollector;
import org.commonjava.maven.cartographer.ops.fn.ProjectProjector;
import org.commonjava.maven.cartographer.request.MetadataCollationRequest;
import org.commonjava.maven.cartographer.request.MetadataExtractionRequest;
import org.commonjava.maven.cartographer.request.MetadataUpdateRequest;
import org.commonjava.maven.cartographer.request.ProjectGraphRequest;
import org.commonjava.maven.cartographer.result.MetadataEntry;
import org.commonjava.maven.cartographer.result.MetadataResult;
import org.commonjava.maven.cartographer.result.ProjectListResult;
import org.commonjava.maven.cartographer.util.RecipeResolver;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MetadataOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected RecipeResolver recipeResolver;

    @Inject
    protected ArtifactManager artifacts;

    @Inject
    protected MavenPomReader pomReader;

    @Inject
    protected MetadataScannerSupport scannerSupport;

    @Inject
    protected DiscoverySourceManager sourceManager;

    @Inject
    protected ResolveOps resolver;

    @Inject
    protected CalculationOps calculations;

    @Inject
    protected RelationshipGraphFactory graphFactory;

    protected MetadataOps()
    {
    }

    public MetadataOps( final ArtifactManager artifacts, final MavenPomReader pomReader,
                        final MetadataScannerSupport scannerSupport, final DiscoverySourceManager sourceManager,
                        final ResolveOps resolver, final CalculationOps calculations,
                        final RelationshipGraphFactory graphFactory, final RecipeResolver dtoResolver )
    {
        this.artifacts = artifacts;
        this.pomReader = pomReader;
        this.scannerSupport = scannerSupport;
        this.sourceManager = sourceManager;
        this.resolver = resolver;
        this.calculations = calculations;
        this.graphFactory = graphFactory;
        this.recipeResolver = dtoResolver;
    }

    public MetadataResult getMetadata( final MetadataExtractionRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final MetadataResult result = new MetadataResult();
        final Set<String> keys = recipe.getKeys();

        final ProjectProjector<Map<String, String>> extractor = ( ref, graph ) -> {
            return keys == null || keys.isEmpty() ? graph.getMetadata( ref ) : graph.getMetadata( ref, keys );
        };

        final ProjectCollector<Map<String, String>> consumer = ( ref, metadata ) -> {
            result.addProject( new MetadataEntry( ref, metadata ) );
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                               new MatchingProjectFunction<Map<String, String>>( recipe, extractor,
                                                                                                 consumer ) );
        return result;
    }

    public ProjectListResult updateMetadata( final MetadataUpdateRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipe.setResolve( false );

        final Map<String, String> globalMetadata = recipe.getGlobalMetadata();
        final Map<ProjectVersionRef, Map<String, String>> projectMetadata = recipe.getProjectMetadata();

        final ProjectListResult result = new ProjectListResult();
        final ProjectProjector<ProjectVersionRef> projector =
            ( ref, graph ) -> {
                final Map<String, String> metadata = new HashMap<>();
                if ( globalMetadata != null )
                {
                    metadata.putAll( globalMetadata );
                }

                final Map<String, String> pm = projectMetadata.get( ref );
                if ( pm != null )
                {
                    metadata.putAll( pm );
                }

                try
                {
                    graph.addMetadata( ref, metadata );
                    return ref;
                }
                catch ( final RelationshipGraphException e )
                {
                    logger.error( String.format( "Failed to update metadata for: %s. Reason: %s", ref, e.getMessage() ),
                                  e );
                }

                return null;
            };

        final ProjectCollector<ProjectVersionRef> collector = ( unused, ref ) -> {
            if ( ref != null )
            {
                result.addProject( ref );
            }
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                               new MatchingProjectFunction<ProjectVersionRef>( recipe, projector,
                                                                                               collector ) );
        return result;
    }

    public ProjectListResult rescanMetadata( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipe.setResolve( false );
        final ProjectListResult result = new ProjectListResult();

        recipeResolver.resolve( recipe );
        final List<? extends Location> locations = recipe.getDiscoveryConfig()
                                                         .getLocations();

        if ( locations == null || locations.isEmpty() )
        {
            logger.error( "No source locations available; returning empty result." );
            return result;
        }

        final ProjectProjector<ProjectVersionRef> projector =
            ( ref, graph ) -> {
                Transfer transfer = null;
                MavenPomView pomView = null;
                try
                {
                    transfer = artifacts.retrieveFirst( locations, ref.asPomArtifact() );
                    if ( transfer == null )
                    {
                        logger.error( "Cannot find POM: {} in locations: {}. Skipping for metadata scanning...",
                                      ref.asPomArtifact(), locations );
                    }
                    else
                    {
                        pomView = pomReader.read( ref, transfer, locations );
                    }
                }
                catch ( final TransferException e )
                {
                    logger.error( String.format( "Cannot read: %s from locations: %s. Reason: %s", ref.asPomArtifact(),
                                                 locations, e.getMessage() ), e );
                }
                catch ( final GalleyMavenException e )
                {
                    logger.error( String.format( "Cannot build POM view for: %s. Reason: %s", ref.asPomArtifact(),
                                                 e.getMessage() ), e );
                }

                if ( pomView == null )
                {
                    return null;
                }

                final Map<String, String> allMeta = scannerSupport.scan( ref, locations, pomView, transfer );

                if ( allMeta != null && !allMeta.isEmpty() )
                {
                    try
                    {
                        graph.addMetadata( ref, allMeta );
                    }
                    catch ( final RelationshipGraphException e )
                    {
                        logger.error( String.format( "Failed to update metadata for: %s in: %s. Reason: %s", ref,
                                                     graph, e.getMessage() ), e );
                    }
                }

                return ref;
            };

        final ProjectCollector<ProjectVersionRef> collector = ( unused, ref ) -> {
            if ( ref != null )
            {
                result.addProject( ref );
            }
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe, new MatchingProjectFunction<>( recipe,
                                                                                                          projector,
                                                                                                          collector ) );
        return result;
    }

    public MetadataCollationResult collate( final MetadataCollationRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final Map<Map<String, String>, Set<ProjectVersionRef>> result = new HashMap<>();

        final Set<String> keys = recipe.getKeys();
        if ( keys == null || keys.isEmpty() )
        {
            return new MetadataCollationResult( result );
        }

        final ProjectProjector<Map<String, String>> projector = ( ref, graph ) -> {
            final Map<String, String> metadata = graph.getMetadata( ref, keys );
            for ( final String key : keys )
            {
                if ( !metadata.containsKey( key ) )
                {
                    metadata.put( key, null );
                }
            }
            return metadata;
        };

        final ProjectCollector<Map<String, String>> collector = ( ref, metadata ) -> {
            Set<ProjectVersionRef> collated = result.get( metadata );
            if ( collated == null )
            {
                collated = new HashSet<ProjectVersionRef>();
                result.put( metadata, collated );
            }

            collated.add( ref );
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe, new MatchingProjectFunction<>( recipe,
                                                                                                          projector,
                                                                                                          collector ) );
        return new MetadataCollationResult( result );
    }
}
