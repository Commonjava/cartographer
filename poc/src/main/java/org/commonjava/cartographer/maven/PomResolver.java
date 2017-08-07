package org.commonjava.cartographer.maven;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.apache.camel.OutHeaders;
import org.commonjava.cartographer.core.data.data.global.CartoPackageInfo;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.user.work.RequestId;
import org.commonjava.cartographer.proc.resolve.CartoNodeResolver;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

import static org.commonjava.cartographer.structure.MessageHeaders.REQUEST_ID;

/**
 * Resolve Maven POM files from remote package managers to resolve graph nodes.
 */
@ApplicationScoped
public class PomResolver implements CartoNodeResolver
{

    @Handler
    public PkgVersion resolve( final @Header( REQUEST_ID ) RequestId requestId, final @Body PkgVersion target,
                               final @OutHeaders Map<String, Object> outHeaders )
            throws Exception
    {
        ProjectRef ga = SimpleProjectRef.parse( target.getPackageId().getPackageName() );
        ProjectVersionRef gav = new SimpleProjectVersionRef( ga, target.getVersion() );
        return null;
    }

    @Override
    public CartoPackageInfo getPackageInfo()
    {
        return new MavenPackageInfo();
    }
}
