package org.commonjava.cartographer.any.bsp;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.core.data.work.WorkManager;
import org.commonjava.cartographer.core.structure.MessageHeaders;
import org.commonjava.cartographer.spi.service.BSPBoundaryProcessor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @see BSPBoundaryProcessor
 */
@ApplicationScoped
public class DefaultBSPBoundaryProcessor
        implements BSPBoundaryProcessor
{
    @Inject
    private WorkManager workManager;

    @Override
    @Handler
    public List<WorkId> startNextPhase( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
    {
        // TODO: This is just an example
        outHeaders.put( MessageHeaders.BSP_PHASE_CONTROL, MessageHeaders.BspPhaseControl.BSP_DONE );
        outHeaders.put( MessageHeaders.BSP_DONE_REASON, MessageHeaders.BspDoneReason.TRAVERSE_COMPLETE );

        return Collections.emptyList();
    }
}
