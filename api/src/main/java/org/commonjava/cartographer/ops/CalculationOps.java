/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    GraphDifference<ProjectRelationship<?, ?>> difference( GraphAnalysisRequest request )
                    throws CartoDataException, CartoRequestException;

    GraphDifference<ProjectVersionRef> intersectingTargetDrift( GraphAnalysisRequest request )
                    throws CartoDataException, CartoRequestException;

    GraphCalculation calculate( MultiGraphRequest request )
                    throws CartoDataException, CartoRequestException;
}
