package org.commonjava.cartographer.core.data.db;

import org.commonjava.cartgorapher.model.graph.PkgId;
import org.commonjava.cartgorapher.model.graph.PkgVersion;
import org.commonjava.cartgorapher.model.graph.Relationship;
import org.commonjava.cartgorapher.model.graph.RelationshipId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Main interface for accessing information about graph structures and their metadata. NOTE: This does NOT contain
 * global working state (traversal information) or user request information (version selections for a traverse).
 */
public interface GraphDB
{
    SortedSet<PkgVersion> getVersionsOfPackage( PkgId pkgId );

    List<Relationship> getRelationshipsFromVersion( PkgVersion version );

    Set<Relationship> getRelationshipsToPackage( PkgVersion version );

    Map<String, Object> getMetadataOfPackage( PkgId id );

    Map<String, Object> getMetadataOfVersion( PkgVersion version );

    Map<String, Object> getMetadataOfRelationship( RelationshipId id );

    Relationship getRelationship( RelationshipId rel );

    boolean isResolved( PkgVersion version );

}
