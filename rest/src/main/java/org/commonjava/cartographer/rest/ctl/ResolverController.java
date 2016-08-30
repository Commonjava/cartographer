/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.rest.ctl;

import org.apache.commons.io.IOUtils;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.conf.CartographerConfig;
import org.commonjava.cartographer.graph.GraphResolver;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.util.RecipeHelper;
import org.commonjava.propulsor.deploy.undertow.util.ApplicationStatus;

import javax.inject.Inject;
import java.io.InputStream;

public class ResolverController
{

    @Inject
    private GraphResolver resolver;

    @Inject
    private CartographerConfig config;

    @Inject
    private RecipeHelper configHelper;

    public void resolve( final InputStream stream )
            throws CartoRESTException
    {
        final MultiGraphRequest recipe = configHelper.readRecipe( stream, MultiGraphRequest.class );

        resolve( recipe );
    }

    public void resolve( final MultiGraphRequest recipe )
        throws CartoRESTException
    {
        recipe.setDefaultPreset( config.getDefaultWebFilterPreset() );

        try
        {
            // TODO: we could provide some feedback to the user based on the graph...
            resolver.resolveGraphs( recipe, ( graph ) -> {
                IOUtils.closeQuietly( graph );
            } );
        }
        catch ( final CartoDataException e )
        {
            throw new CartoRESTException( "Failed to lookup incomplete subgraphs for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new CartoRESTException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                          recipe, e.getMessage() );
        }
    }

}
