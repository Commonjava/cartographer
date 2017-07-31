package org.commonjava.cartographer.proc.resolve;

import org.apache.camel.RecipientList;
import org.commonjava.cartographer.data.global.CartoPackageInfo;
import org.commonjava.cartographer.data.model.PkgId;
import org.commonjava.cartographer.structure.Routes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Map;

/**
 * Dynamically load the available package-specific node resolvers, then determine which of these to use to resolve the
 * target {@link org.commonjava.cartographer.data.model.PkgId}.
 */
// TODO: In a distributed scenario, we'd use something like Infinispan to register the available packageType -> route map
@ApplicationScoped
public class NodeResolverRecipientList
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
                    return new String[]{pkgInfo.getResolverRoute()};
            }
        }

        return new String[]{ Routes.ROUTE_RESOLVER_NOT_FOUND };
    }
}
