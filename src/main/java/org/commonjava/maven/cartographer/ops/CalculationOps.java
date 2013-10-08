package org.commonjava.maven.cartographer.ops;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphCalculation.Type;
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

    public GraphDifference difference( final GraphDescription from, final GraphDescription to )
        throws CartoDataException
    {
        final EProjectWeb firstWeb = data.getProjectWeb( from.getFilter(), from.getRootsArray() );

        final EProjectWeb secondWeb = data.getProjectWeb( to.getFilter(), to.getRootsArray() );

        final Set<ProjectRelationship<?>> firstAll = firstWeb.getAllRelationships();
        final Set<ProjectRelationship<?>> secondAll = secondWeb.getAllRelationships();

        final Set<ProjectRelationship<?>> added = new HashSet<ProjectRelationship<?>>( secondAll );
        added.removeAll( firstAll );

        final Set<ProjectRelationship<?>> removed = new HashSet<ProjectRelationship<?>>( firstAll );
        removed.removeAll( secondAll );

        return new GraphDifference( from, to, added, removed );
    }

    public GraphCalculation subtract( final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( Type.SUBTRACT, graphs );
    }

    public GraphCalculation add( final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( Type.ADD, graphs );
    }

    public GraphCalculation intersection( final List<GraphDescription> graphs )
        throws CartoDataException
    {
        return calculate( Type.INTERSECT, graphs );
    }

    public GraphCalculation calculate( final Type type, final List<GraphDescription> graphs )
        throws CartoDataException
    {
        Set<ProjectRelationship<?>> result = null;
        for ( final GraphDescription graph : graphs )
        {
            final EProjectWeb web = data.getProjectWeb( graph.getFilter(), graph.getRootsArray() );

            if ( web == null )
            {
                throw new CartoDataException( "Cannot retrieve web for: %s.", graph );
            }

            if ( result == null )
            {
                result = new HashSet<>( web.getAllRelationships() );
            }
            else
            {
                switch ( type )
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

        return new GraphCalculation( type, graphs, result );
    }

}
