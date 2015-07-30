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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.AndFilter;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.GraphExport;
import org.commonjava.maven.cartographer.dto.GraphResult;
import org.commonjava.maven.cartographer.dto.ProjectGraphRecipe;
import org.commonjava.maven.cartographer.dto.ProjectGraphRelationshipsRecipe;
import org.commonjava.maven.cartographer.dto.SingleGraphResolverRecipe;
import org.commonjava.maven.cartographer.util.ProjectVersionRefComparator;
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

    public GraphResult<List<ProjectVersionRef>> listProjects( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<List<ProjectVersionRef>> extractor = ( final RelationshipGraph graph ) -> {
            final ProjectVersionRef ref = recipe.getProject();
            if ( ref != null )
            {
                return graph.containsGraph( ref ) ? Collections.singletonList( ref ) : Collections.emptyList();
            }

            final Set<ProjectVersionRef> projects = graph.getAllProjects();

            List<ProjectVersionRef> matching;

            final String matcher = recipe.getProjectGavPattern();
            if ( matcher != null )
            {
                matching = new ArrayList<>();
                for ( final ProjectVersionRef pvr : projects )
                {
                    if ( pvr.toString()
                            .matches( matcher ) )
                    {
                        matching.add( pvr );
                    }
                }
            }
            else
            {
                matching = new ArrayList<>( projects );
            }

            if ( !matching.isEmpty() )
            {
                Collections.sort( matching, new ProjectVersionRefComparator() );
            }

            return matching;
        };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    public GraphResult<Map<ProjectVersionRef, String>> getProjectErrors( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        return getAllProjectErrors( recipe );
    }

    public GraphResult<Map<ProjectVersionRef, String>> getAllProjectErrors( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<Map<ProjectVersionRef, String>> extractor = ( final RelationshipGraph graph ) -> {
            final ProjectVersionRef ref = recipe.getProject();
            if ( ref != null )
            {
                final String error = graph.getProjectError( ref );
                return error == null ? Collections.emptyMap() : Collections.singletonMap( ref, error );
            }

            final Map<ProjectVersionRef, String> allErrors = graph.getAllProjectErrors();
            if ( allErrors == null || allErrors.isEmpty() )
            {
                return Collections.emptyMap();
            }

            final List<ProjectVersionRef> projects = new ArrayList<>( allErrors.keySet() );
            Collections.sort( projects, new ProjectVersionRefComparator() );

            final LinkedHashMap<ProjectVersionRef, String> result = new LinkedHashMap<>();
            final String matcher = recipe.getProjectGavPattern();
            for ( final ProjectVersionRef project : projects )
            {
                if ( StringUtils.isEmpty( matcher ) || project.toString()
                                                              .equals( matcher ) )
                {
                    result.put( project, allErrors.get( project ) );
                }
            }

            return result;
        };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    public GraphResult<Map<ProjectVersionRef, ProjectVersionRef>> getProjectParent( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<Map<ProjectVersionRef, ProjectVersionRef>> extractor =
            ( final RelationshipGraph graph ) -> {
                final ProjectVersionRef ref = recipe.getProject();
                if ( ref != null )
                {
                    final Collection<? extends ProjectRelationship<?>> relationships =
                        graph.getRelationshipsDeclaring( ref );
                    if ( relationships != null && !relationships.isEmpty() )
                    {
                        final ProjectRelationship<?> rel = relationships.iterator()
                                                                        .next();
                        return Collections.singletonMap( ref, rel.getTarget() );
                    }
                    else
                    {
                        return Collections.emptyMap();
                    }
                }

                final String matcher = recipe.getProjectGavPattern();
                final Set<ProjectRelationship<?>> rels = graph.getAllRelationships();
                final Map<ProjectVersionRef, ProjectVersionRef> result = new HashMap<>();
                for ( final ProjectRelationship<?> rel : rels )
                {
                    final ProjectVersionRef project = rel.getDeclaring();
                    if ( StringUtils.isEmpty( matcher ) || project.toString()
                                                                  .equals( matcher ) )
                    {
                        result.put( project, rel.getTarget() );
                    }
                }

                return result;
            };

        return getGraphRefs( ParentFilter.EXCLUDE_TERMINAL_PARENTS, recipe, extractor );
    }

    public GraphResult<Map<ProjectVersionRef, Set<ProjectRelationship<?>>>> getDirectRelationshipsFrom( final ProjectGraphRelationshipsRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<Map<ProjectVersionRef, Set<ProjectRelationship<?>>>> extractor =
            ( graph ) -> {
                final List<ProjectVersionRef> projects = new ArrayList<>();

                final ProjectVersionRef ref = recipe.getProject();
                if ( ref != null )
                {
                    projects.add( ref );
                }

                if ( projects.isEmpty() )
                {
                    projects.addAll( graph.getAllProjects() );
                    Collections.sort( projects, new ProjectVersionRefComparator() );
                }

                final Map<ProjectVersionRef, Set<ProjectRelationship<?>>> result = new LinkedHashMap<>();
                for ( final ProjectVersionRef project : projects )
                {
                    final Set<ProjectRelationship<?>> rels =
                        graph.findDirectRelationshipsFrom( project, recipe.isManagedIncluded(),
                                                           recipe.isConcreteIncluded(), recipe.toTypeArray() );

                    if ( rels != null && !rels.isEmpty() )
                    {
                        result.put( project, rels );
                    }
                }

                return result;
            };

        return getGraphRefs( recipe.getTypeFilter(), recipe, extractor );
    }

    public GraphResult<Map<ProjectVersionRef, Set<? extends ProjectRelationship<?>>>> getDirectRelationshipsTo( final ProjectGraphRelationshipsRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<Map<ProjectVersionRef, Set<? extends ProjectRelationship<?>>>> extractor =
            ( graph ) -> {
                final List<ProjectVersionRef> projects = new ArrayList<>();

                final ProjectVersionRef ref = recipe.getProject();
                if ( ref != null )
                {
                    projects.add( ref );
                }

                if ( projects.isEmpty() )
                {
                    projects.addAll( graph.getAllProjects() );
                    Collections.sort( projects, new ProjectVersionRefComparator() );
                }

                final Map<ProjectVersionRef, Set<? extends ProjectRelationship<?>>> result = new LinkedHashMap<>();
                for ( final ProjectVersionRef project : projects )
                {
                    final Set<ProjectRelationship<?>> rels =
                        graph.findDirectRelationshipsTo( project, recipe.isManagedIncluded(),
                                                         recipe.isConcreteIncluded(), recipe.toTypeArray() );

                    if ( rels != null && !rels.isEmpty() )
                    {
                        result.put( project, rels );
                    }
                }

                return result;
            };

        return getGraphRefs( recipe.getTypeFilter(), recipe, extractor );
    }

    public GraphResult<List<ProjectVersionRef>> reindex( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        return doReindex( recipe, true );
    }

    public GraphResult<List<ProjectVersionRef>> reindexAll( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        return doReindex( recipe, false );
    }

    private GraphResult<List<ProjectVersionRef>> doReindex( final ProjectGraphRecipe recipe,
                                                            final boolean useRecipeFilters )
        throws CartoDataException
    {
        recipe.setResolve( false );
        final GraphRefExtractor<List<ProjectVersionRef>> extractor =
            ( graph ) -> {
                final List<ProjectVersionRef> result = new ArrayList<>();

                if ( graph == null )
                {
                    return result;
                }

                if ( useRecipeFilters )
                {
                    final ProjectVersionRef ref = recipe.getProject();
                    if ( ref != null )
                    {
                        result.add( ref );
                    }
                }

                if ( result.isEmpty() )
                {
                    result.addAll( graph.getAllProjects() );
                    Collections.sort( result, new ProjectVersionRefComparator() );
                }

                final String matcher = recipe.getProjectGavPattern();
                for ( final Iterator<ProjectVersionRef> it = result.iterator(); it.hasNext(); )
                {
                    final ProjectVersionRef project = it.next();
                    if ( !useRecipeFilters || StringUtils.isEmpty( matcher ) || project.toString()
                                                                                       .matches( matcher ) )
                    {
                        try
                        {
                            graph.reindex( project );
                        }
                        catch ( final RelationshipGraphConnectionException e )
                        {
                            logger.error( String.format( "Failed to re-index %s in: %s", project,
                                                         recipe.getWorkspaceId() ), e );
                            it.remove();
                        }
                    }
                }

                return result;
            };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    public GraphResult<Set<ProjectVersionRef>> getIncomplete( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<Set<ProjectVersionRef>> extractor = ( graph ) -> {
            final Set<ProjectVersionRef> projects = graph.getIncompleteSubgraphs();
            final ProjectVersionRef ref = recipe.getProject();
            if ( ref != null )
            {
                projects.retainAll( Collections.singleton( ref ) );
                return projects;
            }

            final String matcher = recipe.getProjectGavPattern();
            for ( final Iterator<ProjectVersionRef> it = projects.iterator(); it.hasNext(); )
            {
                final ProjectVersionRef project = it.next();
                if ( StringUtils.isNotEmpty( matcher ) && !project.toString()
                                                                  .matches( matcher ) )
                {
                    it.remove();
                }
            }

            return projects;
        };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    public GraphResult<Set<ProjectVersionRef>> getVariable( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<Set<ProjectVersionRef>> extractor = ( graph ) -> {
            final Set<ProjectVersionRef> projects = graph.getVariableSubgraphs();
            final ProjectVersionRef ref = recipe.getProject();

            if ( ref != null )
            {
                projects.retainAll( Collections.singleton( ref ) );
                return projects;
            }

            final String matcher = recipe.getProjectGavPattern();
            for ( final Iterator<ProjectVersionRef> it = projects.iterator(); it.hasNext(); )
            {
                final ProjectVersionRef project = it.next();
                if ( StringUtils.isNotEmpty( matcher ) && !project.toString()
                                                                  .matches( matcher ) )
                {
                    it.remove();
                }
            }

            return projects;
        };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    public GraphResult<Map<ProjectVersionRef, List<ProjectVersionRef>>> getAncestry( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<Map<ProjectVersionRef, List<ProjectVersionRef>>> extractor =
            ( graph ) -> {
                final Map<ProjectVersionRef, List<ProjectVersionRef>> result = new LinkedHashMap<>();

                final List<ProjectVersionRef> projects = new ArrayList<ProjectVersionRef>();

                final ProjectVersionRef ref = recipe.getProject();
                if ( ref != null )
                {
                    projects.add( ref );
                }

                if ( projects.isEmpty() )
                {
                    projects.addAll( graph.getAllProjects() );
                    Collections.sort( projects, new ProjectVersionRefComparator() );
                }

                final String matcher = recipe.getProjectGavPattern();
                for ( final ProjectVersionRef project : projects )
                {
                    if ( StringUtils.isEmpty( matcher ) || project.toString()
                                                                  .matches( matcher ) )
                    {
                        List<ProjectVersionRef> ancestry;
                        try
                        {
                            ancestry = CartoGraphUtils.getAncestry( project, graph );

                            if ( ancestry == null )
                            {
                                ancestry = Collections.emptyList();
                            }

                            result.put( project, projects );
                        }
                        catch ( final RelationshipGraphException e )
                        {
                            logger.error( String.format( "Failed to retrieve ancestry of: %s in: %s", project,
                                                         recipe.getWorkspaceId() ), e );
                        }
                    }
                }

                return result;
            };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    public GraphResult<BuildOrder> getBuildOrder( final ProjectGraphRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<BuildOrder> extractor =
            ( graph ) -> {
                final List<ProjectVersionRef> projects = new ArrayList<>();
                final ProjectVersionRef ref = recipe.getProject();
                if ( ref != null )
                {
                    projects.add( ref );
                }

                if ( projects.isEmpty() )
                {
                    projects.addAll( graph.getRoots() );
                    Collections.sort( projects, new ProjectVersionRefComparator() );
                }

                final BuildOrderTraversal traversal = new BuildOrderTraversal();
                for ( final ProjectVersionRef project : projects )
                {
                    try
                    {
                        graph.traverse( project, traversal, TraversalType.breadth_first );
                    }
                    catch ( final RelationshipGraphException e )
                    {
                        logger.error( String.format( "Failed to traverse graph: %s to discover build order for: %s",
                                                     recipe.getWorkspaceId(), project ), e );
                    }
                }

                return traversal.getBuildOrder();
            };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    public GraphResult<GraphExport> exportGraph( final SingleGraphResolverRecipe recipe )
        throws CartoDataException
    {
        final GraphRefExtractor<GraphExport> extractor = ( graph ) -> {
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

            return new GraphExport( rels, missing, variable, errors, allCycles );
        };

        return getGraphRefs( AnyFilter.INSTANCE, recipe, extractor );
    }

    private <M> GraphResult<M> getGraphRefs( final ProjectRelationshipFilter filter,
                                             final SingleGraphResolverRecipe recipe,
                                             final GraphRefExtractor<M> extractor )
        throws CartoDataException
    {
        for ( final GraphDescription desc : recipe.getGraphComposition() )
        {
            desc.setFilter( new AndFilter( filter, desc.filter() ) );
        }

        final LinkedHashMap<GraphDescription, ViewParams> paramMap = resolveOps.resolve( recipe );

        final GraphDescription original = recipe.getGraph();

        final ViewParams params = paramMap.get( original );
        final GraphDescription resolved = new GraphDescription( params.getFilter(), params.getRoots() );

        M matching = null;
        try (RelationshipGraph graph = graphFactory.open( params, false ))
        {
            matching = extractor.extract( graph );
        }
        catch ( final IOException e )
        {
            throw new CartoDataException( "Failed to close graph: {}", e, resolved );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to query graph: {}", e, resolved );
        }

        return new GraphResult<M>( original, resolved, matching );
    }

    @FunctionalInterface
    public interface GraphRefExtractor<M>
    {
        M extract( RelationshipGraph graph );
    }

}
