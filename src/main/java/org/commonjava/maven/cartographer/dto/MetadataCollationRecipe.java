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
package org.commonjava.maven.cartographer.dto;

import java.util.Set;
import java.util.TreeSet;

public class MetadataCollationRecipe
    extends ResolverRecipe
{

    private Set<String> keys;

    public Set<String> getKeys()
    {
        return keys;
    }

    public void setKeys( final Set<String> keys )
    {
        this.keys = new TreeSet<>( keys );
    }

    @Override
    public void normalize()
    {
        super.normalize();
        normalize( keys );
    }

}
