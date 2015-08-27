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

import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.MultiRenderRequest;

import java.util.Map;

public class MultiRenderRequestBuilder<T extends MultiRenderRequestBuilder<T, R>, R extends MultiRenderRequest>
    extends MultiGraphRequestBuilder<T, R>
{

    protected GraphComposition graphs;

    private Map<String, String> params;

    public static final class StandaloneMR
        extends MultiRenderRequestBuilder<StandaloneMR, MultiRenderRequest>
    {
    }

    public static StandaloneMR newMultiRenderRecipeBuilder()
    {
        return new StandaloneMR();
    }

    public T withRenderParameters( final Map<String, String> params )
    {
        this.params = params;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new MultiRenderRequest();
        configure( recipe );

        return recipe;
    }

    protected void configure( final R recipe )
    {
        recipe.setRenderParams( params );
        super.configure( recipe );
    }

}
