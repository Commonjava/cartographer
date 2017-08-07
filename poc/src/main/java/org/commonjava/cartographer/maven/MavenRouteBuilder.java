package org.commonjava.cartographer.maven;

import org.commonjava.cartographer.core.data.data.global.CartoPackageInfo;
import org.commonjava.cartographer.structure.CartoPackageRouter;

import javax.enterprise.context.ApplicationScoped;

/**
 * Created by jdcasey on 7/10/17.
 */
@ApplicationScoped
public class MavenRouteBuilder extends CartoPackageRouter
{
    @Override
    protected CartoPackageInfo getPackageInfo()
    {
        return new MavenPackageInfo();
    }

    @Override
    protected Class<PomResolver> getNodeResolver()
    {
        return PomResolver.class;
    }

    protected Class<GAVSelector> getNodeSelector()
    {
        return GAVSelector.class;
    }
}
