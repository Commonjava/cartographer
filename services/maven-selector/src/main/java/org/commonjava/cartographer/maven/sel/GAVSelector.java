package org.commonjava.cartographer.maven.sel;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.commonjava.cartographer.core.data.dto.SelectionRequest;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartographer.maven.MavenPackageInfo;
import org.commonjava.cartographer.spi.service.NodeSelector;

import javax.enterprise.context.ApplicationScoped;

/**
 * Weigh the user-requested managed versions, previously-selected versions for the GA in the current traversal, and
 * the requested GAV. Then, make a decision about what GAV / POM to resolve. This will also take account of SNAPSHOT
 * and other meta-versions, resolving them to concrete POMs for resolution.
 */
@ApplicationScoped
public class GAVSelector
        implements NodeSelector
{
    @Handler
    @Override
    public PkgVersion select( final @Body SelectionRequest request )
            throws Exception
    {
        return null;
    }

    @Override
    public CartoPackageInfo getPackageInfo()
    {
        return new MavenPackageInfo();
    }
}
