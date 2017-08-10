package org.commonjava.cartgorapher.model.graph;

import org.commonjava.cartgorapher.model.user.TraverseScope;

import java.util.Set;

/**
 * Some representation of a directed relationship between a source {@link PkgVersion}
 * and a target {@link PkgId}, possibly containing version advice.
 */
public interface Relationship
{
    RelationshipId getId();

    PkgVersion getSource();

    PkgId getTarget();

    Set<TraverseScope> getSupportedTraverseScopes();

    String getTargetVersionAdvice();

    Set<PkgId> getExclusions();
}
