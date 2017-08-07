package org.commonjava.cartographer.maven;

import org.commonjava.cartographer.core.data.data.global.CartoPackageInfo;
import org.commonjava.cartographer.core.data.data.model.PkgId;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.user.work.RequestWorkspace;
import org.commonjava.cartographer.proc.select.CartoNodeSelector;

/**
 * Weigh the user-requested managed versions, previously-selected versions for the GA in the current traversal, and
 * the requested GAV. Then, make a decision about what GAV / POM to resolve. This will also take account of SNAPSHOT
 * and other meta-versions, resolving them to concrete POMs for resolution.
 */
public class GAVSelector
        implements CartoNodeSelector
{
    @Override
    public PkgVersion select( final PkgId nodeId, final String versionAdvice, final RequestWorkspace ws )
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
