package org.commonjava.cartographer.core.data.data.global;

/**
 * Describes the route names for a given package type, to enable Recipient-List EIP for node selection and resolution.
 */
public interface CartoPackageInfo
{
    String getResolverRoute();

    String getSelectorRoute();

    String getPackageType();
}
