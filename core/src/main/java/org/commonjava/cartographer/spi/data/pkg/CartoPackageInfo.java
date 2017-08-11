package org.commonjava.cartographer.spi.data.pkg;

import java.io.Serializable;

/**
 * Describes the route names for a given package type, to enable Recipient-List EIP for node selection and resolution.
 */
public interface CartoPackageInfo
        extends Serializable
{
    String getResolverRoute();

    String getSelectorRoute();

    String getPackageType();
}
