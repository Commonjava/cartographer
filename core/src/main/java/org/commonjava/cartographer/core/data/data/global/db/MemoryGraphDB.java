package org.commonjava.cartographer.core.data.data.global.db;

import org.commonjava.cartographer.core.data.data.model.PkgId;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.model.RelationshipId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * In-memory reference implementation of {@link GraphDB}. This will probably serve as a decent model if we want to create
 * a db based on Infinispan.
 */
public class MemoryGraphDB
        implements GraphDB
{
    private Map<PkgId, TreeSet<PkgVersion>> versionsOfPackages = new HashMap<>();

    private Map<PkgVersion, List<Relationship>> relationshipsFromVersions = new HashMap<>();

    private Map<PkgId, Set<Relationship>> relationshipsToPackages = new HashMap<>();

    private Map<PkgId, Map<String, Object>> metadataOfPackages = new HashMap<>();

    private Map<PkgVersion, Map<String, Object>> metadataOfVersions = new HashMap<>();

    private Map<RelationshipId, Map<String, Object>> metadataOfRelationships = new HashMap<>();

    public synchronized void addVersionToPackage( PkgId id, PkgVersion version )
    {
        if ( versionsOfPackages.containsKey( id ) )
        {
            versionsOfPackages.get( id ).add( version );
        }
        else
        {
            versionsOfPackages.put( id, new TreeSet<>( Arrays.asList( version ) ) );
        }
    }

    public synchronized void addRelationships( List<Relationship> relationships )
    {
        Map<PkgVersion, List<Relationship>> relsBySource = new HashMap<>();
        Map<PkgId, Set<Relationship>> relsByTarget = new HashMap<>();

        relationships.forEach( r-> {
            List<Relationship> bySource = relsBySource.computeIfAbsent( r.getSource(), v -> new ArrayList<>() );
            Set<Relationship> byTarget = relsByTarget.computeIfAbsent( r.getTarget(), i -> new HashSet<>() );

            if ( bySource.indexOf( r ) < 0 )
            {
                bySource.add( r );
            }

            byTarget.add( r );
        });

        relationshipsFromVersions.putAll( relsBySource );
        relsByTarget.forEach( (id,rels)->{
            if ( relationshipsToPackages.containsKey( id ) )
            {
                relationshipsToPackages.get( id ).addAll( rels );
            }
            else
            {
                relationshipsToPackages.put( id, rels );
            }
        } );
    }

    @Override
    public SortedSet<PkgVersion> getVersionsOfPackage( PkgId pkgId )
    {
        TreeSet<PkgVersion> versions = versionsOfPackages.get( pkgId );
        return versions == null ? new TreeSet<>() : new TreeSet<>( versions );
    }

    @Override
    public List<Relationship> getRelationshipsFromVersion( PkgVersion version )
    {
        List<Relationship> rels = relationshipsFromVersions.get( version );
        return rels == null ? Collections.emptyList() : new ArrayList<>( rels );
    }

    @Override
    public Set<Relationship> getRelationshipsToPackage( PkgVersion version )
    {
        Set<Relationship> rels = relationshipsToPackages.get( version );
        return rels == null ? Collections.emptySet() : new HashSet<>( rels );
    }



    @Override
    public Map<String, Object> getMetadataOfPackage( PkgId id )
    {
        Map<String, Object> metadata = metadataOfPackages.get( id );
        return metadata == null ? Collections.emptyMap() : new HashMap<>( metadata );
    }

    @Override
    public Map<String, Object> getMetadataOfVersion( PkgVersion version )
    {
        Map<String, Object> metadata = metadataOfVersions.get( version );
        return metadata == null ? Collections.emptyMap() : new HashMap<>( metadata );
    }

    @Override
    public Map<String, Object> getMetadataOfRelationship( RelationshipId id )
    {
        Map<String, Object> metadata = metadataOfRelationships.get( id );
        return metadata == null ? Collections.emptyMap() : new HashMap<>( metadata );
    }

}
