package org.commonjava.cartographer.ops;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.GraphCalculation;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.result.GraphDifference;

/**
 * Created by jdcasey on 8/14/15.
 */
public interface CalculationOps
{
    GraphDifference<ProjectRelationship<?>> difference( GraphAnalysisRequest request )
                    throws CartoDataException, CartoRequestException;

    GraphDifference<ProjectVersionRef> intersectingTargetDrift( GraphAnalysisRequest request )
                    throws CartoDataException, CartoRequestException;

    GraphCalculation calculate( MultiGraphRequest request )
                    throws CartoDataException, CartoRequestException;
}
