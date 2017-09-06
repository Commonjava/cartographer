package org.commonjava.cartographer.core.proc;

import org.apache.camel.Handler;
import org.commonjava.cartgorapher.model.RequestId;
import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartographer.core.data.db.WorkDB;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkItem;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * After the package-specific node selection process ({@link org.commonjava.cartographer.spi.service.NodeSelector}),
 * this class runs to register the selected versions in the
 * {@link org.commonjava.cartographer.core.data.work.RequestWorkspace}.
 */
@ApplicationScoped
public class NodeSelectionRegistrar
{
    @Inject
    private WorkDB workDB;

    @Handler
    public void registerVersionSelection( WorkId workId )
            throws Exception
    {
        WorkItem workItem = workDB.getWorkItem( workId );
        PkgVersion selectedVersion = workItem.getSelected();
        PkgId selectedPkg = selectedVersion.getPackageId();
        PkgId targetPkg = workItem.getTarget();

        RequestId requestId = workId.getRequestId();

        workDB.addVersionSelection( requestId, selectedPkg, selectedVersion );
        workDB.addVersionSelection( requestId, targetPkg, selectedVersion );
    }
}
