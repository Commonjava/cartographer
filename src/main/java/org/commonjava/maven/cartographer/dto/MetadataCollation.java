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
        final Set<MetadataCollationEntry> entries = new HashSet<>();
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
