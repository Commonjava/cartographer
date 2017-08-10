package org.commonjava.cartographer.maven.rsolv;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.commonjava.cartographer.core.data.dto.ResolutionResult;
import org.commonjava.cartographer.core.data.dto.SelectionResult;
import org.commonjava.cartographer.maven.MavenPackageInfo;
import org.commonjava.cartographer.spi.data.CartoPackageInfo;
import org.commonjava.cartographer.spi.service.NodeResolver;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

import javax.enterprise.context.ApplicationScoped;

/**
 * Resolve Maven POM files from remote package managers to resolve graph nodes.
 */
@ApplicationScoped
public class PomResolver implements NodeResolver
{

    @Handler
    public ResolutionResult resolve( final @Body SelectionResult request )
            throws Exception
    {
        ProjectRef ga = SimpleProjectRef.parse( request.getToNode().getPackageId().getPackageName() );
        ProjectVersionRef gav = new SimpleProjectVersionRef( ga, request.getToNode().getVersion() );
        return null;
    }

    @Override
    public CartoPackageInfo getPackageInfo()
    {
        return new MavenPackageInfo();
    }
}
