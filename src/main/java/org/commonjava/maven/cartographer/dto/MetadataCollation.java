/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
