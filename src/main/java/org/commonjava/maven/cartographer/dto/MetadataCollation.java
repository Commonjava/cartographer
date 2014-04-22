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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MetadataCollation
    implements Iterable<MetadataCollationEntry>
{

    private Set<MetadataCollationEntry> collations;

    public MetadataCollation()
    {
    }

    public MetadataCollation( final Map<Map<String, String>, Set<ProjectVersionRef>> collations )
    {
        final Set<MetadataCollationEntry> entries = new HashSet<MetadataCollationEntry>();
        for ( final Entry<Map<String, String>, Set<ProjectVersionRef>> entry : collations.entrySet() )
        {
            final Map<String, String> key = entry.getKey();
            final Set<ProjectVersionRef> value = entry.getValue();

            entries.add( new MetadataCollationEntry( key, value ) );
        }

        this.collations = entries;
    }

    @Override
    public Iterator<MetadataCollationEntry> iterator()
    {
        return collations.iterator();
    }

    public Set<MetadataCollationEntry> getCollations()
    {
        return collations;
    }

    public void setCollations( final Set<MetadataCollationEntry> collations )
    {
        this.collations = collations;
    }

}
