package org.commonjava.cartographer.core.data.db;

import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartgorapher.model.graph.Relationship;
import org.commonjava.cartgorapher.model.graph.RelationshipId;

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

    /**
     * In this implementation, presence of a sourceUri for a PkgVersion is used as a proxy for whether that version has
     * been resolved.
     */
    private Map<PkgVersion, String> sourceUris = new HashMap<>();

    private Map<RelationshipId, Relationship> relationships = new HashMap<>();

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

    public synchronized void addRelationships( List<Relationship> rels )
    {
        Map<PkgVersion, List<Relationship>> relsBySource = new HashMap<>();
        Map<PkgId, Set<Relationship>> relsByTarget = new HashMap<>();

        rels.forEach( r-> {
            RelationshipId rid = r.getId();
            relationships.put( r.getId(), r );
            List<Relationship> bySource = relsBySource.computeIfAbsent( r.getSource(), v -> new ArrayList<>() );
            Set<Relationship> byTarget = relsByTarget.computeIfAbsent( r.getTarget(), i -> new HashSet<>() );

            if ( bySource.indexOf( r ) < 0 )
            {
                bySource.add( r );
            }

            byTarget.add( r );
        });

        relationshipsFromVersions.putAll( relsBySource );
        relsByTarget.forEach( (id,rs)->{
            if ( relationshipsToPackages.containsKey( id ) )
            {
                relationshipsToPackages.get( id ).addAll( rs );
            }
            else
            {
                relationshipsToPackages.put( id, rs );
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
        if ( rels == null )
        {
            return null;
        }

        return rels;
    }

    @Override
    public Set<Relationship> getRelationshipsToPackage( PkgVersion version )
    {
        Set<Relationship> rels = relationshipsToPackages.get( version );
        if ( rels == null )
        {
            return null;
        }

        return rels;
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

    @Override
    public Relationship getRelationship( final RelationshipId relId )
    {
        return relationships.get( relId );
    }

    @Override
    public boolean isResolved( final PkgVersion version )
    {
        return sourceUris.containsKey( version );
    }

}
