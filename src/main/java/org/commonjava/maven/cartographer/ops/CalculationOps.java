/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.GraphDifference;

@ApplicationScoped
public class CalculationOps
{

    @Inject
    private CartoDataManager data;

    protected CalculationOps()
    {
    }

    public CalculationOps( final CartoDataManager data )
    {
        this.data = data;
    }

    public GraphDifference<ProjectRelationship<?>> difference( final GraphDescription from, final GraphDescription to )
        throws CartoDataException
    {
        final EProjectNet firstWeb = data.getProjectWeb( from.getFilter(), from.getRootsArray() );

        final EProjectNet secondWeb = data.getProjectWeb( to.getFilter(), to.getRootsArray() );

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
        final EProjectNet firstWeb = data.getProjectWeb( from.getFilter(), from.getRootsArray() );
        final EProjectNet secondWeb = data.getProjectWeb( to.getFilter(), to.getRootsArray() );

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
        for ( final GraphDescription graph : composition.getGraphs() )
        {
            final EProjectNet web = data.getProjectWeb( graph.getFilter(), graph.getRootsArray() );

            if ( web == null )
            {
                throw new CartoDataException( "Cannot retrieve web for: {}.", graph );
            }

            if ( result == null )
            {
                result = new HashSet<ProjectRelationship<?>>( web.getAllRelationships() );
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
                        break;
                    }
                    case INTERSECT:
                    {
                        result.retainAll( web.getAllRelationships() );
                        break;
                    }
                }
            }
        }

        return new GraphCalculation( composition.getCalculation(), composition.getGraphs(), result );
    }

}
