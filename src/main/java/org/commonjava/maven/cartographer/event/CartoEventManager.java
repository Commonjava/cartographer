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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;

public interface CartoEventManager
{

    void waitForGraph( ProjectVersionRef ref, CartoDataManager data, long timeoutMillis )
        throws CartoDataException;

    void fireErrorEvent( final ProjectRelationshipsErrorEvent evt );

    void fireStorageEvent( final RelationshipStorageEvent evt );

}
