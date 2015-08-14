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
package org.commonjava.cartographer.result;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MetadataCollationResult
    implements Iterable<MetadataCollationEntry>
{

    private Set<MetadataCollationEntry> collations;

    public MetadataCollationResult()
    {
    }

    public MetadataCollationResult( final Map<Map<String, String>, Set<ProjectVersionRef>> collations )
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
