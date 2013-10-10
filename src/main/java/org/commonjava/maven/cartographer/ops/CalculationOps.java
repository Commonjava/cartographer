package org.commonjava.maven.cartographer.ops;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
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

    public GraphDifference difference( final GraphDescription from, final GraphDescription to )
        throws CartoDataException
    {
        final EProjectNet firstWeb = data.getProjectWeb( from.getFilter(), from.getRootsArray() );

        final EProjectNet secondWeb = data.getProjectWeb( to.getFilter(), to.getRootsArray() );

        final Collection<ProjectRelationship<?>> firstAll = firstWeb.getAllRelationships();
        final Collection<ProjectRelationship<?>> secondAll = secondWeb.getAllRelationships();

        final Set<ProjectRelationship<?>> added = new HashSet<ProjectRelationship<?>>( secondAll );
        added.removeAll( firstAll );

        final Set<ProjectRelationship<?>> removed = new HashSet<ProjectRelationship<?>>( firstAll );
        removed.removeAll( secondAll );

        return new GraphDifference( from, to, added, removed );
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
                throw new CartoDataException( "Cannot retrieve web for: %s.", graph );
            }

            if ( result == null )
            {
                result = new HashSet<>( web.getAllRelationships() );
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
