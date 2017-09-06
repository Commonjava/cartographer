package org.commonjava.cartographer.maven.sel;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartographer.core.data.db.WorkDB;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkItem;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartographer.maven.MavenPackageInfo;
import org.commonjava.cartographer.spi.service.NodeSelector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

/**
 * Weigh the user-requested managed versions, previously-selected versions for the GA in the current traversal, and
 * the requested GAV. Then, make a decision about what GAV / POM to resolve. This will also take account of SNAPSHOT
 * and other meta-versions, resolving them to concrete POMs for resolution.
 */
@ApplicationScoped
public class GAVSelector
        implements NodeSelector
{
    @Inject
    private WorkDB workDB;

    @Handler
    @Override
    public void select( final @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
            throws Exception
    {
        WorkItem item = workDB.getWorkItem( workId );
        PkgId pkgId = item.getTarget();

        PkgVersion versionSelection = workDB.getVersionSelection( workId.getRequestId(), pkgId );
        if ( versionSelection == null )
        {
            String targetVersionAdvice = item.getTargetVersionAdvice();
            try
            {
                // TODO: POM relocation will result in PkgId A -> PkgVersion(PkgId B, Version B)

                // resolve maven metadata
                // construct a PackageVersion
                // set in WorkItem
                // register in WorkDB for this requestId
                // Set the out header appropriately
                String sel = "THIS IS THE VERSION SELECTED FROM METADATA";
                versionSelection = new PkgVersion( pkgId, sel );
                item.setSelected( versionSelection );

                // TODO: other outcomes...
                outHeaders.put( MessageHeaders.SELECTION_RESULT, MessageHeaders.SelectionResult.DONE );
            }
            catch ( Exception e )
            {
                item.setError( e );
                outHeaders.put( MessageHeaders.SELECTION_RESULT, MessageHeaders.SelectionResult.FAILED );
            }
        }
        else
        {
            item.setSelected( versionSelection );
            outHeaders.put( MessageHeaders.SELECTION_RESULT, MessageHeaders.SelectionResult.AVOIDED );
        }
    }

    @Override
    public CartoPackageInfo getPackageInfo()
    {
        return new MavenPackageInfo();
    }
}
