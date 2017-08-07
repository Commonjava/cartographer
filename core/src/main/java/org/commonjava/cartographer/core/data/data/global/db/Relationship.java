package org.commonjava.cartographer.core.data.data.global.db;

import org.commonjava.cartographer.core.data.data.model.PkgId;
import org.commonjava.cartographer.core.data.data.model.PkgVersion;
import org.commonjava.cartographer.core.data.data.model.RelationshipId;
import org.commonjava.cartographer.core.data.data.user.TraverseScope;

import java.util.List;
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

    List<String> getTargetVersionAdvice();
}
