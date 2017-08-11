package org.commonjava.cartographer.spi.service;

import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.camel.OutHeaders;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartographer.core.data.work.WorkId;
import org.commonjava.cartographer.spi.data.pkg.CartoPackageInfo;

import java.util.Map;

/**
 * Resolves a specific package version to a node in the graph, possibly by retrieving its metadata from remote package
 * managers. Each {@link NodeResolver} implementation is intended to address a single package type.
 *
 * This processor will respond with a routing header
 * {@link org.commonjava.cartographer.core.structure.MessageHeaders#RESOLUTION_RESULT} and a value from
 * {@link org.commonjava.cartographer.core.structure.MessageHeaders.ResolutionResult} as follows:
 *
 * <ul>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.ResolutionResult#RESOLUTION_ERROR}:
 *          An error occurred while attempting to resolve the relationship metadata for the target
 *          {@link org.commonjava.cartgorapher.model.graph.PkgVersion}</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.ResolutionResult#RESOLUTION_FAILED}:
 *          Relationship metadata could not be found for the target {@link org.commonjava.cartgorapher.model.graph.PkgVersion}.
 *          <b>NOTE:</b> This is <b>NOT</b> the same as saying the target has no relationships. Rather, it indicates the
 *          metadata for that target cannot be found at all, not even something indicating it has no relationships.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.ResolutionResult#RESOLUTION_AVOIDED}:
 *          The target was previously marked as resolved in the {@link org.commonjava.cartographer.core.data.db.GraphDB}.
 *          No further action is required from the resolver.</li>
 *     <li>{@link org.commonjava.cartographer.core.structure.MessageHeaders.ResolutionResult#RESOLUTION_DONE}: The
 *          target is ready for traversal.</li>
 * </ul>
 *
 * At some point in the future, it may be necessary to add another out-header that will specify whether the resolution
 * actually took place during this processor's execution, or if it had been completed previously (and avoided here).
 *
 * Resolution should <b>ONLY</b> be attempted if the node isn't already marked as resolved via
 * {@link org.commonjava.cartographer.core.data.db.GraphDB#isResolved(PkgVersion)}.
 */
public interface NodeResolver
{
    @Handler
    void resolve( @Body WorkId workId, @OutHeaders Map<String, Object> outHeaders )
        throws Exception;

    CartoPackageInfo getPackageInfo();
}
