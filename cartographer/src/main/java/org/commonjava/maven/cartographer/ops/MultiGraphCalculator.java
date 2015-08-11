package org.commonjava.maven.cartographer.ops;

import org.apache.commons.io.IOUtils;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.request.GraphCalculation;
import org.commonjava.maven.cartographer.request.GraphComposition;
import org.commonjava.maven.cartographer.request.GraphDescription;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 8/11/15.
 */
public class MultiGraphCalculator
{

    @Inject
    private RelationshipGraphFactory graphFactory;

    public MultiGraphCalculator(){}

    public MultiGraphCalculator( RelationshipGraphFactory factory )
    {
        this.graphFactory = factory;
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
