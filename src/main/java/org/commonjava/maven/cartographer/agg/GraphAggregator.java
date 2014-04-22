/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.agg;

import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.cartographer.data.CartoDataException;

public interface GraphAggregator
{

    EProjectGraph connectIncomplete( EProjectGraph graph, AggregationOptions crawlerConfig )
        throws CartoDataException;

    EProjectNet connectIncomplete( EProjectWeb graph, AggregationOptions crawlerConfig )
        throws CartoDataException;

}
