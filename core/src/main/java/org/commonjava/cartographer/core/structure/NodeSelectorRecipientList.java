package org.commonjava.cartographer.core.structure;

import org.apache.camel.RecipientList;
import org.commonjava.cartographer.core.data.data.global.CartoPackageInfo;
import org.commonjava.cartographer.core.data.data.model.PkgId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

/**
 * Dynamically load the available package-specific node selectors, then determine which of these to use to resolve the
 * target {@link PkgId}.
 */
// TODO: In a distributed scenario, we'd use something like Infinispan to register the available packageType -> route map
@ApplicationScoped
public class NodeSelectorRecipientList
{
    @Inject
    // FIXME ISPN Cache
    private Map<String, CartoPackageInfo> packageTypes;

    @RecipientList
    public String[] route( PkgId target )
    {
        if ( packageTypes != null )
        {
            CartoPackageInfo pkgInfo = packageTypes.get( target.getPackageType() );
            if ( pkgInfo != null )
            {
                return new String[]{pkgInfo.getSelectorRoute()};
            }
        }

        return new String[]{ Routes.ROUTE_SELECTOR_NOT_FOUND };
    }
}
