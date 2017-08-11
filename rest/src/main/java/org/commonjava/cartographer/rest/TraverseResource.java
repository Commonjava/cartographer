package org.commonjava.cartographer.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cartgorapher.model.RequestId;
import org.commonjava.cartgorapher.model.user.UserRequest;
import org.commonjava.cartographer.core.data.work.RequestWorkspace;
import org.commonjava.cartographer.core.data.work.WorkManager;
import org.commonjava.propulsor.deploy.resteasy.RestResources;
import org.jboss.resteasy.annotations.Body;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Provides access to launch new graph traverses, find status of traverses, etc.
 */
@ApplicationScoped
@Path( "/api/traverse" )
public class TraverseResource
        implements RestResources
{
    @Inject
    private WorkManager workManager;

    @Inject
    ObjectMapper mapper;

    @POST
    public RequestId startTraverse( @Context HttpServletRequest request )
            throws IOException
    {
        UserRequest userRequest = mapper.readValue( request.getInputStream(), UserRequest.class );
        RequestWorkspace ws = new RequestWorkspace( userRequest );

        workManager.addRequestWorkspace( ws );
        // FIXME: Fire off a new exchnage into Camel.

        return ws.getRequestId();
    }
}
