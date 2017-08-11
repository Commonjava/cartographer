package org.commonjava.cartographer.maven.sel;

import org.commonjava.cartographer.maven.MavenPackageInfo;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;
import org.commonjava.cartographer.spi.route.PackageSelectorRoute;
import org.commonjava.cartographer.spi.service.NodeSelector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by jdcasey on 8/8/17.
 */
@ApplicationScoped
public class GAVSelectorRoute
        extends PackageSelectorRoute
{
    @Inject
    private GAVSelector selector;

    @Override
    protected CartoPackageInfo getPackageInfo()
    {
        return new MavenPackageInfo();
    }

    @Override
    protected NodeSelector getNodeSelector()
    {
        return selector;
    }
}
