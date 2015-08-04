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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;
import org.commonjava.maven.cartographer.dto.GraphExport;
import org.commonjava.maven.cartographer.ops.fn.GraphFunction;
import org.commonjava.maven.cartographer.ops.fn.MatchingProjectFunction;
import org.commonjava.maven.cartographer.ops.fn.ProjectCollector;
import org.commonjava.maven.cartographer.ops.fn.ProjectProjector;
import org.commonjava.maven.cartographer.ops.fn.ProjectSelector;
import org.commonjava.maven.cartographer.ops.fn.ValueHolder;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRecipe;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRelationshipsRecipe;
import org.commonjava.maven.cartographer.recipe.SingleGraphResolverRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: For GAV and GAV-pattern filtering, we need to do more to handle snapshots
@ApplicationScoped
public class GraphOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected RelationshipGraphFactory graphFactory;

    @Inject
    protected ResolveOps resolveOps;

    @Inject
    protected CalculationOps calcOps;

    protected GraphOps()
    {
    }

    public GraphOps( final RelationshipGraphFactory graphFactory, final ResolveOps resolveOps,
                     final CalculationOps calcOps )
    {
        this.graphFactory = graphFactory;
        this.resolveOps = resolveOps;
        this.calcOps = calcOps;
    }

    public List<ProjectVersionRef> listProjects( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final List<ProjectVersionRef> result = new ArrayList<>();

        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> {
            return graph.containsGraph( ref ) ? ref : null;
        };

        final ProjectCollector<ProjectVersionRef> consumer = ( unused, ref ) -> {
            if ( ref != null )
            {
                result.add( ref );
            }
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<ProjectVersionRef>( recipe, extractor,
                                                                                                  consumer ) );
        return result;
    }

    public Map<ProjectVersionRef, String> getProjectErrors( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        return getAllProjectErrors( recipe );
    }

    public Map<ProjectVersionRef, String> getAllProjectErrors( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final LinkedHashMap<ProjectVersionRef, String> result = new LinkedHashMap<>();

        final ProjectProjector<String> extractor = ( ref, graph ) -> {
            final String error = graph.getProjectError( ref );
            if ( StringUtils.isEmpty( error ) )
            {
                return null;
            }

            return error;
        };

        final ProjectCollector<String> consumer = ( ref, error ) -> {
            if ( error != null )
            {
                result.put( ref, error );
            }
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<String>( recipe, extractor, consumer ) );

        return result;
    }

    public Map<ProjectVersionRef, ProjectVersionRef> getProjectParent( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final Map<ProjectVersionRef, ProjectVersionRef> result = new HashMap<>();

        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> {
            final Set<ProjectRelationship<?>> rels = graph.getDirectRelationships( ref );
            for ( final ProjectRelationship<?> rel : rels )
            {
                if ( rel instanceof ParentRelationship )
                {
                    return rel.getTarget();
                }
            }

            return null;
        };

        final ProjectCollector<ProjectVersionRef> consumer = ( ref, parent ) -> {
            result.put( ref, parent );
        };

        resolveOps.resolveAndExtractSingleGraph( ParentFilter.EXCLUDE_TERMINAL_PARENTS, recipe,
                                                 new MatchingProjectFunction<ProjectVersionRef>( recipe, extractor,
                                                                                                  consumer ) );

        return result;
    }

    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> getDirectRelationshipsFrom( final ProjectGraphRelationshipsRecipe recipe )
        throws CartoDataException
    {
        final Map<ProjectVersionRef, Set<ProjectRelationship<?>>> result = new LinkedHashMap<>();

        final ProjectProjector<Set<ProjectRelationship<?>>> extractor =
            ( ref, graph ) -> {
                final Set<ProjectRelationship<?>> rels =
                    graph.findDirectRelationshipsFrom( ref, recipe.isManagedIncluded(), recipe.isConcreteIncluded(),
                                                       recipe.toTypeArray() );
                return rels == null || rels.isEmpty() ? null : new HashSet<>( rels );
            };

        final ProjectCollector<Set<ProjectRelationship<?>>> consumer = ( ref, rels ) -> {
            if ( rels != null )
            {
                result.put( ref, rels );
            }
        };

        resolveOps.resolveAndExtractSingleGraph( recipe.getTypeFilter(), recipe,
                                                 new MatchingProjectFunction<Set<ProjectRelationship<?>>>( recipe,
                                                                                                            extractor,
                                                                                                            consumer ) );
        return result;
    }

    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> getDirectRelationshipsTo( final ProjectGraphRelationshipsRecipe recipe )
        throws CartoDataException
    {
        final Map<ProjectVersionRef, Set<ProjectRelationship<?>>> result = new LinkedHashMap<>();

        final ProjectProjector<Set<ProjectRelationship<?>>> extractor =
            ( ref, graph ) -> {
                final Set<ProjectRelationship<?>> rels =
                    graph.findDirectRelationshipsTo( ref, recipe.isManagedIncluded(), recipe.isConcreteIncluded(),
                                                     recipe.toTypeArray() );
                return rels == null || rels.isEmpty() ? null : new HashSet<>( rels );
            };

        final ProjectCollector<Set<ProjectRelationship<?>>> consumer = ( ref, rels ) -> {
            if ( rels != null )
            {
                result.put( ref, rels );
            }
        };

        resolveOps.resolveAndExtractSingleGraph( recipe.getTypeFilter(), recipe,
                                                 new MatchingProjectFunction<Set<ProjectRelationship<?>>>( recipe,
                                                                                                            extractor,
                                                                                                            consumer ) );
        return result;
    }

    public List<ProjectVersionRef> reindex( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        return doReindex( recipe, true );
    }

    private List<ProjectVersionRef> doReindex( final ProjectGraphRecipe recipe,
                                                            final boolean useRecipeFilters )
        throws CartoDataException
    {
        recipe.setResolve( false );
        if ( !useRecipeFilters )
        {
            recipe.getGraph()
                  .setFilter( AnyFilter.INSTANCE );
        }

        final List<ProjectVersionRef> result = new ArrayList<>();
        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> {
            try
            {
                graph.reindex( ref );
                return ref;
            }
            catch ( final RelationshipGraphConnectionException e )
            {
                logger.error( String.format( "Failed to re-index %s in: %s", ref, recipe.getWorkspaceId() ), e );
            }

            return null;
        };

        final ProjectCollector<ProjectVersionRef> consumer = ( unused, ref ) -> {
            if ( ref != null )
            {
                result.add( ref );
            }
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<ProjectVersionRef>( recipe, extractor,
                                                                                                  consumer ) );

        return result;
    }

    public Set<ProjectVersionRef> getIncomplete( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> result = new HashSet<>();
        final ProjectProjector<ProjectVersionRef> extractor = (ref, graph)->{
            return ref;
        };
        
        final ProjectCollector<ProjectVersionRef> consumer = (unused, ref)->{
            result.add( ref );
        };
        
        final ProjectSelector supplier = (graph)->{
            return graph.getIncompleteSubgraphs();
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe, new MatchingProjectFunction<ProjectVersionRef>( recipe, extractor, consumer, supplier ) );
        return result;
    }

    public Set<ProjectVersionRef> getVariable( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> result = new HashSet<>();
        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> {
            return ref;
        };

        final ProjectCollector<ProjectVersionRef> consumer = ( unused, ref ) -> {
            result.add( ref );
        };

        final ProjectSelector supplier = ( graph ) -> {
            return graph.getVariableSubgraphs();
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<ProjectVersionRef>( recipe, extractor,
                                                                                                  consumer, supplier ) );
        return result;
    }

    public Map<ProjectVersionRef, List<ProjectVersionRef>> getAncestry( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final Map<ProjectVersionRef, List<ProjectVersionRef>> result = new LinkedHashMap<>();
        final ProjectProjector<List<ProjectVersionRef>> extractor =
            ( ref, graph ) -> {
                try
                {
                    return CartoGraphUtils.getAncestry( ref, graph );
                }
                catch ( final RelationshipGraphException e )
                {
                    logger.error( String.format( "Failed to retrieve ancestry of: %s in: %s", ref,
                                                 recipe.getWorkspaceId() ), e );
                }

                return Collections.emptyList();
            };

        final ProjectCollector<List<ProjectVersionRef>> consumer = ( ref, ancestry ) -> {
            result.put( ref, ancestry );
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<List<ProjectVersionRef>>( recipe,
                                                                                                        extractor,
                                                                                                        consumer ) );

        return result;
    }

    public BuildOrder getBuildOrder( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final BuildOrderTraversal traversal = new BuildOrderTraversal();
        final ProjectProjector<ProjectVersionRef> extractor =
            ( ref, graph ) -> {
                try
                {
                    graph.traverse( ref, traversal, TraversalType.breadth_first );
                    return ref;
                }
                catch ( final RelationshipGraphException e )
                {
                    logger.error( String.format( "Failed to traverse graph: %s to discover build order for: %s",
                                                 recipe.getWorkspaceId(), ref ), e );
                }

                return null;
            };

        final ProjectCollector<ProjectVersionRef> consumer = ( ref, ref2 ) -> {
        };

        final ProjectSelector supplier = ( graph ) -> {
            return graph.getRoots();
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<ProjectVersionRef>( recipe, extractor,
                                                                                                  consumer, supplier ) );
        return traversal.getBuildOrder();
    }

    public GraphExport exportGraph( final SingleGraphResolverRecipe recipe )
        throws CartoDataException
    {
        final ValueHolder<GraphExport> holder = new ValueHolder<>();
        final GraphFunction extractor = ( graph ) -> {
            final Set<ProjectRelationship<?>> rels = graph.getAllRelationships();
            final Set<ProjectVersionRef> missing = graph.getAllIncompleteSubgraphs();
            final Set<ProjectVersionRef> variable = graph.getAllVariableSubgraphs();

            final Map<ProjectVersionRef, String> errors = graph.getAllProjectErrors();

            final Set<List<ProjectRelationship<?>>> allCycles = new HashSet<>();

            final Set<EProjectCycle> cycles = graph.getCycles();
            if ( cycles != null )
            {
                for ( final EProjectCycle cycle : cycles )
                {
                    allCycles.add( new ArrayList<>( cycle.getAllRelationships() ) );
                }
            }

            holder.consumer()
                  .accept( new GraphExport( rels, missing, variable, errors, allCycles ) );
        };

        resolveOps.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe, extractor );
        return holder.getValue();
    }

}
