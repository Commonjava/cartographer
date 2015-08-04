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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.GraphDifference;
import org.commonjava.maven.cartographer.recipe.RecipeResolver;

@ApplicationScoped
public class CalculationOps
{

    @Inject
    protected RelationshipGraphFactory graphFactory;

    @Inject
    protected RecipeResolver dtoResolver;

    protected CalculationOps()
    {
    }

    public CalculationOps( final RelationshipGraphFactory graphFactory, final RecipeResolver dtoResolver )
    {
        this.graphFactory = graphFactory;
        this.dtoResolver = dtoResolver;
    }

    public GraphDifference<ProjectRelationship<?>> difference( final GraphDescription from, final GraphDescription to,
                                                               final String workspaceId )
        throws CartoDataException
    {
        dtoResolver.resolvePresets( from );
        dtoResolver.resolvePresets( to );

        final ManagedDependencyMutator mutator = new ManagedDependencyMutator();

        RelationshipGraph firstWeb = null;
        RelationshipGraph secondWeb = null;
        final Collection<ProjectRelationship<?>> firstAll;
        final Collection<ProjectRelationship<?>> secondAll;
        try
        {
            try
            {
                ViewParams params = from.view();
                if ( params == null )
                {
                    params = new ViewParams( workspaceId, from.filter(), mutator, from.rootsArray() );
                }

                firstWeb = graphFactory.open( params, false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException(
                                              "Failed to retrieve graph from workspace: '{}' for description: {}. Reason: {}",
                                              e, workspaceId, from, e.getMessage() );
            }
            try
            {
                ViewParams params = to.view();
                if ( params == null )
                {
                    params = new ViewParams( workspaceId, to.filter(), mutator, to.rootsArray() );
                }

                secondWeb = graphFactory.open( params, false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException(
                                              "Failed to retrieve graph from workspace: '{}' for description: {}. Reason: {}",
                                              e, workspaceId, to, e.getMessage() );
            }
            firstAll = firstWeb.getAllRelationships();
            secondAll = secondWeb.getAllRelationships();
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( firstWeb );
            CartoGraphUtils.closeGraphQuietly( secondWeb );
        }

        final Set<ProjectRelationship<?>> removed = new HashSet<ProjectRelationship<?>>( firstAll );
        removed.removeAll( secondAll );

        final Set<ProjectRelationship<?>> added = new HashSet<ProjectRelationship<?>>( secondAll );
        added.removeAll( firstAll );

        return new GraphDifference<ProjectRelationship<?>>( from, to, added, removed );
    }

    public GraphDifference<ProjectVersionRef> intersectingTargetDrift( final GraphDescription from,
                                                                       final GraphDescription to,
                                                                       final String workspaceId )
        throws CartoDataException
    {
        dtoResolver.resolvePresets( from );
        dtoResolver.resolvePresets( to );

        final ManagedDependencyMutator mutator = new ManagedDependencyMutator();
        RelationshipGraph firstWeb = null;
        RelationshipGraph secondWeb = null;

        final Map<ProjectRef, Set<ProjectVersionRef>> firstAll;
        final Map<ProjectRef, Set<ProjectVersionRef>> secondAll;
        try
        {
            try
            {
                ViewParams params = from.view();
                if ( params == null )
                {
                    params = new ViewParams( workspaceId, from.filter(), mutator, from.rootsArray() );
                }

                firstWeb = graphFactory.open( params, false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException(
                                              "Failed to retrieve graph from workspace: '{}' for description: {}. Reason: {}",
                                              e, workspaceId, from, e.getMessage() );
            }
            try
            {
                ViewParams params = to.view();
                if ( params == null )
                {
                    params = new ViewParams( workspaceId, to.filter(), mutator, to.rootsArray() );
                }

                secondWeb = graphFactory.open( params, false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException(
                                              "Failed to retrieve graph from workspace: '{}' for description: {}. Reason: {}",
                                              e, workspaceId, to, e.getMessage() );
            }
            firstAll = mapTargetsToGA( firstWeb );
            secondAll = mapTargetsToGA( secondWeb );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( firstWeb );
            CartoGraphUtils.closeGraphQuietly( secondWeb );
        }

        reduceToIntersection( firstAll, secondAll );

        final Set<ProjectVersionRef> removed = new HashSet<ProjectVersionRef>();
        for ( final Set<ProjectVersionRef> refSet : firstAll.values() )
        {
            if ( refSet != null )
            {
                removed.addAll( refSet );
            }
        }

        final Set<ProjectVersionRef> added = new HashSet<ProjectVersionRef>();
        for ( final Set<ProjectVersionRef> refSet : secondAll.values() )
        {
            if ( refSet != null )
            {
                added.addAll( refSet );
            }
        }

        return new GraphDifference<ProjectVersionRef>( from, to, added, removed );
    }

    private void reduceToIntersection( final Map<ProjectRef, Set<ProjectVersionRef>> first,
                                       final Map<ProjectRef, Set<ProjectVersionRef>> second )
    {
        for ( final ProjectRef ref : new HashSet<ProjectRef>( first.keySet() ) )
        {
            if ( !second.containsKey( ref ) )
            {
                first.remove( ref );
            }
        }

        for ( final ProjectRef ref : new HashSet<ProjectRef>( second.keySet() ) )
        {
            if ( !first.containsKey( ref ) )
            {
                second.remove( ref );
            }
        }
    }

    private Map<ProjectRef, Set<ProjectVersionRef>> mapTargetsToGA( final RelationshipGraph graph )
    {
        final Map<ProjectRef, Set<ProjectVersionRef>> result = new HashMap<ProjectRef, Set<ProjectVersionRef>>();
        for ( final ProjectVersionRef ref : graph.getAllProjects() )
        {
            final ProjectRef pr = ref.asProjectRef();

            Set<ProjectVersionRef> pvrs = result.get( pr );
            if ( pvrs == null )
            {
                pvrs = new HashSet<ProjectVersionRef>();
                result.put( pr, pvrs );
            }

            pvrs.add( ref );
        }

        return result;
    }

    public GraphCalculation calculate( final GraphComposition composition, final String workspaceId )
        throws CartoDataException
    {
        dtoResolver.resolvePresets( composition );
        final Map<GraphDescription, ViewParams> paramMap = new HashMap<GraphDescription, ViewParams>();

        final GraphMutator mutator = new ManagedDependencyMutator();
        for ( final GraphDescription desc : composition )
        {
            paramMap.put( desc, new ViewParams( workspaceId, desc.filter(), mutator, desc.rootsArray() ) );
        }

        return calculateFromParamMap( composition, paramMap );
    }

    public GraphCalculation calculateFromParamMap( final GraphComposition composition,
                                                   final Map<GraphDescription, ViewParams> paramMap )
        throws CartoDataException
    {
        final Map<GraphDescription, RelationshipGraph> graphMap = new HashMap<>();
        try
        {
            for ( final GraphDescription desc : composition )
            {
                final ViewParams params = paramMap.get( desc );
                RelationshipGraph graph;
                try
                {
                    graph = graphFactory.open( params, false );
                }
                catch ( final RelationshipGraphException e )
                {
                    throw new CartoDataException( "Failed to open graph: {}. Reason: {}", e, params, e.getMessage() );
                }
                graphMap.put( desc, graph );
            }

            return calculateFromGraphMap( composition, graphMap );
        }
        finally
        {
            for ( final RelationshipGraph graph : graphMap.values() )
            {
                IOUtils.closeQuietly( graph );
            }
        }
    }

    public GraphCalculation calculateFromGraphMap( final GraphComposition composition,
                                                   final Map<GraphDescription, RelationshipGraph> graphMap )
        throws CartoDataException
    {
        Set<ProjectRelationship<?>> result = null;
        Set<ProjectVersionRef> roots = null;

        out: for ( final GraphDescription desc : composition.getGraphs() )
        {
            final RelationshipGraph graph = graphMap.get( desc );

            if ( graph == null )
            {
                throw new CartoDataException( "Cannot retrieve web for: {}.", graph );
            }

            if ( result == null )
            {
                result = new HashSet<>( graph.getAllRelationships() );
                roots = new HashSet<>( graph.getRoots() );
            }
            else
            {
                switch ( composition.getCalculation() )
                {
                    case SUBTRACT:
                    {
                        result.removeAll( graph.getAllRelationships() );

                        if ( result.isEmpty() )
                        {
                            break out;
                        }

                        break;
                    }
                    case ADD:
                    {
                        result.addAll( graph.getAllRelationships() );
                        roots.addAll( graph.getRoots() );
                        break;
                    }
                    case INTERSECT:
                    {
                        result.retainAll( graph.getAllRelationships() );
                        roots.addAll( graph.getRoots() );

                        if ( result.isEmpty() )
                        {
                            break out;
                        }

                        break;
                    }
                }
            }
        }

        return new GraphCalculation( composition.getCalculation(), composition.getGraphs(), roots, result );
    }

}
