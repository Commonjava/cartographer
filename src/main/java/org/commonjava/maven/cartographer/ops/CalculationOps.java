/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.ops;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.GraphDifference;

@ApplicationScoped
public class CalculationOps
{

    @Inject
    protected RelationshipGraphFactory graphFactory;

    protected CalculationOps()
    {
    }

    public CalculationOps( final RelationshipGraphFactory graphFactory )
    {
        this.graphFactory = graphFactory;
    }

    public GraphDifference<ProjectRelationship<?>> difference( final GraphDescription from, final GraphDescription to,
                                                               final String workspaceId )
        throws CartoDataException
    {
        final ManagedDependencyMutator mutator = new ManagedDependencyMutator();

        RelationshipGraph firstWeb = null;
        RelationshipGraph secondWeb = null;
        final Collection<ProjectRelationship<?>> firstAll;
        final Collection<ProjectRelationship<?>> secondAll;
        try
        {
            try
            {
                firstWeb =
                    graphFactory.open( new ViewParams( workspaceId, from.getFilter(), mutator, from.getRootsArray() ),
                                       false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException(
                                              "Failed to retrieve graph from workspace: '{}' for description: {}. Reason: {}",
                                              e, workspaceId, from, e.getMessage() );
            }
            try
            {
                secondWeb =
                    graphFactory.open( new ViewParams( workspaceId, to.getFilter(), mutator, to.getRootsArray() ),
                                       false );
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

    public GraphDifference<ProjectVersionRef> intersectingTargetDrift( final String workspaceId,
                                                                       final GraphDescription from,
                                                                       final GraphDescription to )
        throws CartoDataException
    {
        final ManagedDependencyMutator mutator = new ManagedDependencyMutator();
        RelationshipGraph firstWeb = null;
        RelationshipGraph secondWeb = null;

        final Map<ProjectRef, Set<ProjectVersionRef>> firstAll;
        final Map<ProjectRef, Set<ProjectVersionRef>> secondAll;
        try
        {
            try
            {
                firstWeb =
                    graphFactory.open( new ViewParams( workspaceId, from.getFilter(), mutator, from.getRootsArray() ),
                                       false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException(
                                              "Failed to retrieve graph from workspace: '{}' for description: {}. Reason: {}",
                                              e, workspaceId, from, e.getMessage() );
            }
            try
            {
                secondWeb =
                    graphFactory.open( new ViewParams( workspaceId, to.getFilter(), mutator, to.getRootsArray() ),
                                       false );
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

    public GraphCalculation subtract( final String workspaceId, final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( new GraphComposition( Type.SUBTRACT, graphs ), workspaceId );
    }

    public GraphCalculation add( final String workspaceId, final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( new GraphComposition( Type.ADD, graphs ), workspaceId );
    }

    public GraphCalculation intersection( final String workspaceId, final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( new GraphComposition( Type.INTERSECT, graphs ), workspaceId );
    }

    public GraphCalculation calculate( final GraphComposition composition, final String workspaceId )
        throws CartoDataException
    {
        Set<ProjectRelationship<?>> result = null;
        Set<ProjectVersionRef> roots = null;

        final GraphMutator mutator = new ManagedDependencyMutator();

        for ( final GraphDescription desc : composition.getGraphs() )
        {
            RelationshipGraph graph = null;
            try
            {
                try
                {
                    graph =
                        graphFactory.open( new ViewParams( workspaceId, desc.getFilter(), mutator, desc.getRootsArray() ),
                                           false );
                }
                catch ( final RelationshipGraphException e )
                {
                    throw new CartoDataException(
                                                  "Failed to retrieve graph from workspace: '{}' for description: {}. Reason: {}",
                                                  e, workspaceId, desc, e.getMessage() );
                }
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
                            break;
                        }
                    }
                }
            }
            finally
            {
                CartoGraphUtils.closeGraphQuietly( graph );
            }
        }

        return new GraphCalculation( composition.getCalculation(), composition.getGraphs(), roots, result );
    }

}
