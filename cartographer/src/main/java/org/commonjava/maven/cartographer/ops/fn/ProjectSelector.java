package org.commonjava.maven.cartographer.ops.fn;

import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface ProjectSelector
{

    Set<ProjectVersionRef> getProjects( RelationshipGraph graph );

}
