package org.commonjava.cartographer.spi;

import org.apache.camel.OutHeaders;
import org.commonjava.cartographer.core.data.data.global.CartoPackageInfo;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.user.work.RequestId;

import java.util.Map;

/**
 * Resolves a specific package version to a node in the graph, possibly by retrieving its metadata from remote package
 * managers. Each {@link CartoNodeResolver} implementation is intended to address a single package type.
 */
public interface CartoNodeResolver
{
    PkgVersion resolve( final RequestId requestId, final PkgVersion nodeId, final @OutHeaders
            Map<String, Object> outHeaders )
        throws Exception;

    CartoPackageInfo getPackageInfo();
}
