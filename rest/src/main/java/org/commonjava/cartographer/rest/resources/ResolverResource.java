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
package org.commonjava.cartographer.rest.resources;

import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.ResolverController;
import org.commonjava.propulsor.deploy.resteasy.RestResources;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.application_json;

@Path( "/api/depgraph/graph" )
@Consumes( { application_json } )
public class ResolverResource
        implements RestResources
{

    @Inject
    private ResolverController controller;

    @POST
    public Response resolveGraph( final MultiGraphRequest recipe )
    {
        try
        {
            controller.resolve( recipe );
            return Response.ok().build();

        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }
        return null;
    }

}
