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

import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.GraphDifference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CalculationOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public GraphDifference<ProjectRelationship<?>> difference( final GraphDescription from, final GraphDescription to )
        throws CartoDataException
    {
        final ManagedDependencyMutator mutator = new ManagedDependencyMutator();

        final EProjectNet firstWeb = data.getProjectWeb( from.getFilter(), mutator, from.getRootsArray() );

        final EProjectNet secondWeb = data.getProjectWeb( to.getFilter(), mutator, to.getRootsArray() );

        final Collection<ProjectRelationship<?>> firstAll = firstWeb.getAllRelationships();
        final Collection<ProjectRelationship<?>> secondAll = secondWeb.getAllRelationships();

        final Set<ProjectRelationship<?>> removed = new HashSet<ProjectRelationship<?>>( firstAll );
        removed.removeAll( secondAll );

        final Set<ProjectRelationship<?>> added = new HashSet<ProjectRelationship<?>>( secondAll );
        added.removeAll( firstAll );

        return new GraphDifference<ProjectRelationship<?>>( from, to, added, removed );
    }

    public GraphDifference<ProjectVersionRef> intersectingTargetDrift( final GraphDescription from, final GraphDescription to )
        throws CartoDataException
    {
        final ManagedDependencyMutator mutator = new ManagedDependencyMutator();
        final EProjectNet firstWeb = data.getProjectWeb( from.getFilter(), mutator, from.getRootsArray() );
        final EProjectNet secondWeb = data.getProjectWeb( to.getFilter(), mutator, to.getRootsArray() );

        final Map<ProjectRef, Set<ProjectVersionRef>> firstAll = mapTargetsToGA( firstWeb );
        final Map<ProjectRef, Set<ProjectVersionRef>> secondAll = mapTargetsToGA( secondWeb );

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

    private void reduceToIntersection( final Map<ProjectRef, Set<ProjectVersionRef>> first, final Map<ProjectRef, Set<ProjectVersionRef>> second )
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

    private Map<ProjectRef, Set<ProjectVersionRef>> mapTargetsToGA( final EProjectNet net )
    {
        final Map<ProjectRef, Set<ProjectVersionRef>> result = new HashMap<ProjectRef, Set<ProjectVersionRef>>();
        for ( final ProjectVersionRef ref : net.getAllProjects() )
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

    public GraphCalculation subtract( final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( new GraphComposition( Type.SUBTRACT, graphs ) );
    }

    public GraphCalculation add( final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( new GraphComposition( Type.ADD, graphs ) );
    }

    public GraphCalculation intersection( final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( new GraphComposition( Type.INTERSECT, graphs ) );
    }

    public GraphCalculation calculate( final GraphComposition composition )
        throws CartoDataException
    {
        Set<ProjectRelationship<?>> result = null;
        Set<ProjectVersionRef> roots = null;
        for ( final GraphDescription graph : composition.getGraphs() )
        {
            GraphView view = graph.getView();
            if ( view == null )
            {
                view =
                    new GraphView( data.getCurrentWorkspace(), graph.getFilter(), new ManagedDependencyMutator(),
                                   graph.getRootsArray() );
            }

            logger.info( "Retrieving project web for: {}", view );

            final EProjectNet web = data.getProjectWeb( view );

            if ( web == null )
            {
                throw new CartoDataException( "Cannot retrieve web for: {}.", graph );
            }

            if ( result == null )
            {
                result = new HashSet<>( web.getAllRelationships() );
                roots = new HashSet<>( graph.getRoots() );
            }
            else
            {
                switch ( composition.getCalculation() )
                {
                    case SUBTRACT:
                    {
                        result.removeAll( web.getAllRelationships() );
                        break;
                    }
                    case ADD:
                    {
                        result.addAll( web.getAllRelationships() );
                        roots.addAll( graph.getRoots() );
                        break;
                    }
                    case INTERSECT:
                    {
                        result.retainAll( web.getAllRelationships() );
                        roots.addAll( graph.getRoots() );
                        break;
                    }
                }
            }
        }

        return new GraphCalculation( composition.getCalculation(), composition.getGraphs(), roots, result );
    }

}
