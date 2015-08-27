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
package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphCalculationType;
import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.GraphDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraphCompositionBuilder
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static GraphCompositionBuilder newGraphCompositionBuilder()
    {
        return new GraphCompositionBuilder();
    }

    private GraphCalculationType calculation;

    private final List<GraphDescription> graphs = new ArrayList<>();

    public GraphCompositionBuilder withCalculation( final GraphCalculationType calculation )
    {
        this.calculation = calculation;
        return this;
    }

    public GraphCompositionBuilder withGraph( final GraphDescription description )
    {
        if ( !graphs.isEmpty() && calculation == null )
        {
            calculation = GraphCalculationType.ADD;
        }

        graphs.add( description );
        return this;
    }

    public GraphComposition build()
    {
        return new GraphComposition( calculation, graphs );
    }

}
