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

import org.apache.commons.lang.StringUtils;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.graph.GraphResolver;
import org.commonjava.cartographer.graph.fn.GraphFunction;
import org.commonjava.cartographer.graph.fn.MatchingProjectFunction;
import org.commonjava.cartographer.graph.fn.MultiGraphFunction;
import org.commonjava.cartographer.graph.fn.ProjectCollector;
import org.commonjava.cartographer.graph.fn.ProjectProjector;
import org.commonjava.cartographer.graph.fn.ProjectSelector;
import org.commonjava.cartographer.graph.fn.ValueHolder;
import org.commonjava.cartographer.graph.util.CartoGraphUtils;
import org.commonjava.cartographer.ops.GraphOps;
import org.commonjava.cartographer.request.GraphDescription;
import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.request.SingleGraphRequest;
import org.commonjava.cartographer.result.GraphExport;
import org.commonjava.cartographer.result.MappedProjectRelationships;
import org.commonjava.cartographer.result.MappedProjectRelationshipsResult;
import org.commonjava.cartographer.result.MappedProjectResult;
import org.commonjava.cartographer.result.MappedProjects;
import org.commonjava.cartographer.result.MappedProjectsResult;
import org.commonjava.cartographer.result.ProjectError;
import org.commonjava.cartographer.result.ProjectErrors;
import org.commonjava.cartographer.result.ProjectListResult;
import org.commonjava.cartographer.result.ProjectPath;
import org.commonjava.cartographer.result.ProjectPathsResult;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.PathsTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphOpsImpl
                implements GraphOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphResolver resolver;

    protected GraphOpsImpl()
    {
    }

    public GraphOpsImpl( final GraphResolver resolver )
    {
        this.resolver = resolver;
    }

    @Override
    public ProjectListResult listProjects( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final ProjectListResult result = new ProjectListResult();

        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> graph.containsGraph( ref ) ? ref : null;

        final ProjectCollector<ProjectVersionRef> consumer = ( unused, ref ) -> {
            if ( ref != null )
            {
                result.addProject( ref );
            }
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer ) );
        return result;
    }

    @Override
    public ProjectPathsResult getPaths( final PathsRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        //        Collections.sort( paths, RelationshipPathComparator.INSTANCE );

        ProjectPathsResult result = new ProjectPathsResult();

        final MultiGraphFunction<Set<ProjectRelationship<?, ?>>> extractor = ( allRels, graphMap ) -> {
            for ( final GraphDescription desc : graphMap.keySet() )
            {
                final RelationshipGraph graph = graphMap.get( desc );
                final ProjectRelationshipFilter filter = desc.filter();

                final PathsTraversal paths = new PathsTraversal( filter, recipe.getTargets() );
                try
                {
                    graph.traverse( paths, TraversalType.depth_first );
                }
                catch ( final RelationshipGraphException ex )
                {
                    throw new CartoDataException(
                                    "Failed to open / traverse the graph (for paths operation): " + ex.getMessage(),
                                    ex );
                }

                final Set<List<ProjectRelationship<?, ?>>> discoveredPaths = paths.getDiscoveredPaths();

                for ( final List<ProjectRelationship<?, ?>> path : discoveredPaths )
                {
                    if ( path == null || path.isEmpty() )
                    {
                        continue;
                    }

                    for ( final ProjectRelationship<?, ?> rel : path )
                    {
                        if ( !allRels.contains( rel ) )
                        {
                            // continue to the next path...
                            break;
                        }
                    }

                    List<ProjectRelationship<?, ?>> detachedPath = Conversions.convertToDetachedRelationships( path );
                    final ProjectVersionRef ref = detachedPath.get( path.size() - 1 ).getTarget();
                    result.addPath( ref, new ProjectPath( detachedPath ) );
                }
            }
        };

        resolver.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, recipe,
                                                ( allProjects, allRels, roots ) -> allRels.get(), extractor );

        return result;
    }

    @Override
    public ProjectErrors getProjectErrors( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        return getAllProjectErrors( recipe );
    }

    private ProjectErrors getAllProjectErrors( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final ProjectErrors result = new ProjectErrors();

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
                result.addProject( new ProjectError( ref, error ) );
            }
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer ) );

        return result;
    }

    @Override
    public MappedProjectResult getProjectParent( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        MappedProjectResult result = new MappedProjectResult();

        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> {
            final Set<ProjectRelationship<?, ?>> rels = graph.getDirectRelationships( ref );
            for ( final ProjectRelationship<?, ?> rel : rels )
            {
                if ( rel instanceof ParentRelationship )
                {
                    return rel.getTarget();
                }
            }

            return null;
        };

        final ProjectCollector<ProjectVersionRef> consumer = result::addProject;

        resolver.resolveAndExtractSingleGraph( ParentFilter.EXCLUDE_TERMINAL_PARENTS, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer ) );

        return result;
    }

    @Override
    public MappedProjectRelationshipsResult getDirectRelationshipsFrom( final ProjectGraphRelationshipsRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        MappedProjectRelationshipsResult result = new MappedProjectRelationshipsResult();

        final ProjectProjector<Set<ProjectRelationship<?, ?>>> extractor = ( ref, graph ) -> {
            final Set<ProjectRelationship<?, ?>> rels = graph.findDirectRelationshipsFrom( ref, recipe.isManagedIncluded(),
                                                                                        recipe.isConcreteIncluded(),
                                                                                        recipe.toTypeArray() );
            return rels == null || rels.isEmpty() ? null : new HashSet<>( rels );
        };

        final ProjectCollector<Set<ProjectRelationship<?, ?>>> consumer = ( ref, rels ) -> {
            if ( rels != null )
            {
                result.addProject( new MappedProjectRelationships( ref, rels ) );
            }
        };

        resolver.resolveAndExtractSingleGraph( recipe.getTypeFilter(), recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer ) );
        return result;
    }

    @Override
    public MappedProjectRelationshipsResult getDirectRelationshipsTo( final ProjectGraphRelationshipsRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        MappedProjectRelationshipsResult result = new MappedProjectRelationshipsResult();

        final ProjectProjector<Set<ProjectRelationship<?, ?>>> extractor = ( ref, graph ) -> {
            final Set<ProjectRelationship<?, ?>> rels = graph.findDirectRelationshipsTo( ref, recipe.isManagedIncluded(),
                                                                                      recipe.isConcreteIncluded(),
                                                                                      recipe.toTypeArray() );
            return rels == null || rels.isEmpty() ? null : new HashSet<>( rels );
        };

        final ProjectCollector<Set<ProjectRelationship<?, ?>>> consumer = ( ref, rels ) -> {
            if ( rels != null )
            {
                result.addProject( new MappedProjectRelationships( ref, rels ) );
            }
        };

        resolver.resolveAndExtractSingleGraph( recipe.getTypeFilter(), recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer ) );
        return result;
    }

    @Override
    public ProjectListResult reindex( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        return doReindex( recipe );
    }

    private ProjectListResult doReindex( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipe.setResolve( false );
        if ( recipe.getGraph().filter() == null )
        {
            recipe.getGraph().setFilter( AnyFilter.INSTANCE );
        }

        final ProjectListResult result = new ProjectListResult();
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
                result.addProject( ref );
            }
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer ) );

        return result;
    }

    @Override
    public ProjectListResult getIncomplete( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final ProjectListResult result = new ProjectListResult();
        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> ref;

        final ProjectCollector<ProjectVersionRef> consumer = ( unused, ref ) -> result.addProject( ref );

        final ProjectSelector supplier = RelationshipGraph::getIncompleteSubgraphs;

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer,
                                                                                supplier ) );
        return result;
    }

    @Override
    public ProjectListResult getVariable( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final ProjectListResult result = new ProjectListResult();
        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> ref;

        final ProjectCollector<ProjectVersionRef> consumer = ( unused, ref ) -> result.addProject( ref );

        final ProjectSelector supplier = RelationshipGraph::getVariableSubgraphs;

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer,
                                                                                supplier ) );
        return result;
    }

    @Override
    public MappedProjectsResult getAncestry( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final MappedProjectsResult result = new MappedProjectsResult();
        final ProjectProjector<List<ProjectVersionRef>> extractor = ( ref, graph ) -> {
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

        final ProjectCollector<List<ProjectVersionRef>> consumer =
                        ( ref, mapped ) -> result.addProject( new MappedProjects( ref, mapped ) );

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer ) );

        return result;
    }

    @Override
    public BuildOrder getBuildOrder( final ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final BuildOrderTraversal traversal = new BuildOrderTraversal();
        final ProjectProjector<ProjectVersionRef> extractor = ( ref, graph ) -> {
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

        final ProjectSelector supplier = RelationshipGraph::getRoots;

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe,
                                                 new MatchingProjectFunction<>( recipe, extractor, consumer,
                                                                                supplier ) );
        return traversal.getBuildOrder();
    }

    @Override
    public GraphExport exportGraph( final SingleGraphRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final ValueHolder<GraphExport> holder = new ValueHolder<>();
        final GraphFunction extractor = ( graph ) -> {
            final Set<ProjectRelationship<?, ?>> rels = graph.getAllRelationships();
            final Set<ProjectVersionRef> missing = graph.getAllIncompleteSubgraphs();

            if ( missing != null && missing.containsAll( recipe.getGraph().getRoots() ) )
            {
                holder.consumer().accept( null );
            }

            final Set<ProjectVersionRef> variable = graph.getAllVariableSubgraphs();
            final Set<EProjectCycle> cycles = graph.getCycles();

            final Map<ProjectVersionRef, String> errorMap = graph.getAllProjectErrors();
            ProjectErrors errors = new ProjectErrors();
            for ( ProjectVersionRef key : errorMap.keySet() )
            {
                errors.addProject( new ProjectError( key, errorMap.get( key ) ) );
            }

            holder.consumer().accept( new GraphExport( rels, missing, variable, errors, cycles ) );
        };

        resolver.resolveAndExtractSingleGraph( AnyFilter.INSTANCE, recipe, extractor );
        return holder.get();
    }

}
