package org.commonjava.cartographer.maven.rsolv;

import org.commonjava.cartographer.maven.MavenPackageInfo;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;
import org.commonjava.cartographer.spi.route.PackageResolverRoute;
import org.commonjava.cartographer.spi.service.NodeResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by jdcasey on 8/8/17.
 */
@ApplicationScoped
public class PomResolverRoute
        extends PackageResolverRoute
{
    @Inject
    private PomResolver resolver;

    @Override
    protected CartoPackageInfo getPackageInfo()
    {
        return new MavenPackageInfo();
    }

    @Override
    protected NodeResolver getNodeResolver()
    {
        return resolver;
    }
}
