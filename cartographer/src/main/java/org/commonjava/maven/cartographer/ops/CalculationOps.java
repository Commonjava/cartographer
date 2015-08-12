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

import java.util.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.fn.MultiGraphFunction;
import org.commonjava.maven.cartographer.ops.fn.ValueHolder;
import org.commonjava.maven.cartographer.request.*;
import org.commonjava.maven.cartographer.result.GraphDifference;
import org.commonjava.maven.cartographer.util.RecipeResolver;

@ApplicationScoped
public class CalculationOps
{

    @Inject
    private RelationshipGraphFactory graphFactory;

    @Inject
    private RecipeResolver dtoResolver;

    @Inject
    private MultiGraphCalculator graphCalculator;

    @Inject
    private ResolveOps resolveOps;

    protected CalculationOps()
    {
    }

    public CalculationOps( final MultiGraphCalculator graphCalculator, final RelationshipGraphFactory graphFactory,
                           final RecipeResolver dtoResolver )
    {
        this.graphCalculator = graphCalculator;
        this.graphFactory = graphFactory;
        this.dtoResolver = dtoResolver;
    }

    public GraphDifference<ProjectRelationship<?>> difference( final GraphAnalysisRequest request )
                    throws CartoDataException, CartoRequestException
    {
        List<MultiGraphRequest> requests = request.getGraphRequests();
        if ( requests == null || requests.size() != 2 )
        {
            throw new CartoRequestException( "You must specify exactly 2 graph requests to calculate a difference!" );
        }

        MultiGraphRequest from = requests.get( 0 );
        MultiGraphRequest to = requests.get( 1 );

        ValueHolder<Set<ProjectRelationship<?>>> fromRels = new ValueHolder<>();
        ValueHolder<Set<ProjectRelationship<?>>> toRels = new ValueHolder<>();

        resolveOps.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, from,
                                                ( allProjects, allRelationships, roots ) -> allRelationships.get(),
                                                ( elements, graphs ) -> fromRels.set( elements ) );

        resolveOps.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, to,
                                                ( allProjects, allRelationships, roots ) -> allRelationships.get(),
                                                ( elements, graphs ) -> toRels.set( elements ) );

        final Set<ProjectRelationship<?>> removed = new HashSet<>( fromRels.get() );
        removed.removeAll( toRels.get() );

        final Set<ProjectRelationship<?>> added = new HashSet<>( toRels.get() );
        added.removeAll( fromRels.get() );

        return new GraphDifference<>( from, to, added, removed );
    }

    public GraphDifference<ProjectVersionRef> intersectingTargetDrift( final GraphAnalysisRequest request )
                    throws CartoDataException, CartoRequestException
    {
        List<MultiGraphRequest> requests = request.getGraphRequests();
        if ( requests == null || requests.size() != 2 )
        {
            throw new CartoRequestException( "You must specify exactly 2 graph requests to calculate a difference!" );
        }

        MultiGraphRequest from = requests.get( 0 );
        MultiGraphRequest to = requests.get( 1 );

        ValueHolder<Map<ProjectRef, Set<ProjectVersionRef>>> fromMap = new ValueHolder<>();
        ValueHolder<Map<ProjectRef, Set<ProjectVersionRef>>> toMap = new ValueHolder<>();

        resolveOps.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, from,
                                                ( allProjects, allRelationships, roots ) -> allProjects.get(),
                                                mapTargetsToGA( fromMap ) );

        resolveOps.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, to,
                                                ( allProjects, allRelationships, roots ) -> allProjects.get(),
                                                mapTargetsToGA( toMap ) );

        Map<ProjectRef, Set<ProjectVersionRef>> firstAll = fromMap.get();
        Map<ProjectRef, Set<ProjectVersionRef>> secondAll = toMap.get();
        reduceToIntersection( firstAll, secondAll );

        final Set<ProjectVersionRef> removed = new HashSet<>();
        firstAll.values().stream().filter( refSet -> refSet != null ).forEach( removed::addAll );

        final Set<ProjectVersionRef> added = new HashSet<>();
        secondAll.values().stream().filter( refSet -> refSet != null ).forEach( added::addAll );

        return new GraphDifference<>( from, to, added, removed );
    }

    private void reduceToIntersection( final Map<ProjectRef, Set<ProjectVersionRef>> first,
                                       final Map<ProjectRef, Set<ProjectVersionRef>> second )
    {
        new HashSet<>( first.keySet() ).stream().filter( ref -> !second.containsKey( ref ) ).forEach( first::remove );

        new HashSet<>( second.keySet() ).stream().filter( ref -> !first.containsKey( ref ) ).forEach( second::remove );
    }

    private MultiGraphFunction<Set<ProjectVersionRef>> mapTargetsToGA(
                    ValueHolder<Map<ProjectRef, Set<ProjectVersionRef>>> value )
    {
        final Map<ProjectRef, Set<ProjectVersionRef>> result = new HashMap<>();
        value.set( result );

        return ( elements, graph ) -> elements.forEach( ( ref ) -> {
            final ProjectRef pr = ref.asProjectRef();

            Set<ProjectVersionRef> pvrs = result.get( pr );
            if ( pvrs == null )
            {
                pvrs = new HashSet<>();
                result.put( pr, pvrs );
            }

            pvrs.add( ref );
        } );
    }

    public GraphCalculation calculate( final MultiGraphRequest request )
                    throws CartoDataException, CartoRequestException
    {
        Map<GraphDescription, RelationshipGraph> graphMap = resolveOps.resolveToGraphMap( request );

        return graphCalculator.calculateFromGraphMap( request.getGraphComposition(), graphMap );
    }

}
