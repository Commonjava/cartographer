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
package org.commonjava.maven.cartographer.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;

@ApplicationScoped
@Named( "no-op" )
@Alternative
public class NoOpCartoEventManager
    implements CartoEventManager
{

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final RelationshipGraph graph, final long timeoutMillis )
        throws CartoDataException
    {
    }

    @Override
    public void fireErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
    }

    @Override
    public void fireStorageEvent( final RelationshipStorageEvent evt )
    {
    }

}
