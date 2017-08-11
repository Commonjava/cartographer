package org.commonjava.cartographer.core.data.pkg;

import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;

/**
 * Registry of available package handlers, defined by their corresponding
 * {@link CartoPackageInfo} instances registered here.
 */
public interface PackageInfoRegistry
{
    CartoPackageInfo getPackageInfo( String packageType );
}
