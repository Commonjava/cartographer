package org.commonjava.cartographer.spi;

import org.commonjava.cartographer.core.data.data.global.CartoPackageInfo;
import org.commonjava.cartographer.core.data.data.model.PkgId;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.user.work.RequestWorkspace;

/**
 * Component responsible for weighing user-requested version overrides, previous version selections for similar nodes,
 * and the current node request, then selecting an output node coordinate to resolve.
 */
public interface CartoNodeSelector
{
    PkgVersion select( PkgId nodeId, String versionAdvice, RequestWorkspace ws )
        throws Exception;

    CartoPackageInfo getPackageInfo();
}
