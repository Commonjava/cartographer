package org.commonjava.cartographer.maven;

import org.commonjava.cartographer.data.global.CartoPackageInfo;

/**
 * Defines the packageType for maven nodes, and provides routes for selecting and resolving them.
 */
public class MavenPackageInfo
        implements CartoPackageInfo
{
    public static final String PACKAGE_TYPE_MAVEN = "maven";

    public static final String ROUTE_RESOLVE_MAVEN = "resolve-maven";

    public static final String ROUTE_SELECT_MAVEN = "select-maven";

    @Override
    public String getResolverRoute()
    {
        return ROUTE_RESOLVE_MAVEN;
    }

    @Override
    public String getSelectorRoute()
    {
        return ROUTE_SELECT_MAVEN;
    }

    @Override
    public String getPackageType()
    {
        return PACKAGE_TYPE_MAVEN;
    }
}
