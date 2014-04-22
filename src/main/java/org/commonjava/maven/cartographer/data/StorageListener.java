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
package org.commonjava.maven.cartographer.data;

import java.util.Collection;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public interface StorageListener
{

    void relationshipsStored( Collection<ProjectRelationship<?>> stored, Set<ProjectRelationship<?>> rejected )
        throws CartoDataException;

}
