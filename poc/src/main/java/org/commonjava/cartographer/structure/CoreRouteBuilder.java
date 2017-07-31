package org.commonjava.cartographer.structure;

import org.apache.camel.model.ChoiceDefinition;
import org.commonjava.cartographer.proc.resolve.NodeResolverRecipientList;
import org.commonjava.cartographer.proc.result.NodeResultAggregator;
import org.commonjava.cartographer.proc.select.NodeSelectorRecipientList;
import org.commonjava.cartographer.proc.traverse.NodeTraverser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.cartographer.structure.RouteIds.COLLECT;
import static org.commonjava.cartographer.structure.RouteIds.RESOLVE;
import static org.commonjava.cartographer.structure.RouteIds.SELECT;
import static org.commonjava.cartographer.structure.RouteIds.TRAVERSE;
import static org.commonjava.cartographer.structure.Routes.ROUTE_ADD_NODE_PROGRESS;
import static org.commonjava.cartographer.structure.Routes.ROUTE_ADD_NODE_RESULTS;
import static org.commonjava.cartographer.structure.Routes.ROUTE_NODE_CYCLE_ERROR;
import static org.commonjava.cartographer.structure.Routes.ROUTE_PROGRESS_NODE_ADDED;
import static org.commonjava.cartographer.structure.Routes.ROUTE_PROGRESS_NODE_TRAVERSED;
import static org.commonjava.cartographer.structure.Routes.ROUTE_RESOLVE_NODE_LOOKUP;
import static org.commonjava.cartographer.structure.Routes.ROUTE_SELECT_NODE_LOOKUP;
import static org.commonjava.cartographer.structure.Routes.ROUTE_TRAVERSE_NODE;

@ApplicationScoped
public class CoreRouteBuilder
        extends CartoRouteBuilder
{

    @Inject
    private NodeTraverser analyzer;

    @Inject
    private NodeResultAggregator results;

    @Inject
    private NodeResolverRecipientList resolveTypeRecipientList;

    protected void configure()
    {
        route().routeId( TRAVERSE.name() )
               .from( ROUTE_TRAVERSE_NODE )
               .wireTap( ROUTE_ADD_NODE_PROGRESS )
               .bean( NodeTraverser.class )
               .wireTap( ROUTE_PROGRESS_NODE_TRAVERSED )
               .choice()
               .when( header( RoutingHeaders.ANALYSIS_INCLUDE_NODE.name() ) )
               .to( ROUTE_ADD_NODE_RESULTS );

        ChoiceDefinition resultChoice = route().routeId( COLLECT.name() )
                                               .from( ROUTE_ADD_NODE_RESULTS )
                                               .bean( NodeResultAggregator.class )
                                               .choice();

        resultChoice.when( header( RoutingHeaders.RESOLVE_NODES.name() ) ).split().body().to( ROUTE_RESOLVE_NODE_LOOKUP );
        resultChoice.when( header( RoutingHeaders.NODE_CYCLE.name() ) ).to( ROUTE_NODE_CYCLE_ERROR );

        route().routeId( SELECT.name() )
               .from( ROUTE_SELECT_NODE_LOOKUP )
               .wireTap( ROUTE_PROGRESS_NODE_ADDED )
               .bean( NodeSelectorRecipientList.class );

        route().routeId( RESOLVE.name() )
               .from( ROUTE_RESOLVE_NODE_LOOKUP )
               .wireTap( ROUTE_PROGRESS_NODE_ADDED )
               .bean( NodeResolverRecipientList.class );

//        route().routeId( PROGRESS.name() )
//               .from( ROUTE_PROGRESS_NODE_ADDED, ROUTE_PROGRESS_NODE_TRAVERSED, ROUTE_PROGRESS_NODE_RESOLVED )
//               .bean( RequestProgressCorrelator.class );
//
//        route().routeId( ERRORS.name() ).from( ROUTE_NODE_CYCLE_ERROR, ROUTE_RESOLVER_ERROR ).bean( ErrorCorrelator.class );
    }
}
