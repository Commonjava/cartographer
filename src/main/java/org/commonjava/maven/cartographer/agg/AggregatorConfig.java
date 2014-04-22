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

import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class AggregatorConfig
{

    private final Set<ProjectVersionRef> roots;

    public AggregatorConfig( final Set<ProjectVersionRef> roots )
    {
        this.roots = roots;
    }

    public ProjectVersionRef[] getRoots()
    {
        return roots.toArray( new ProjectVersionRef[] {} );
    }

}
