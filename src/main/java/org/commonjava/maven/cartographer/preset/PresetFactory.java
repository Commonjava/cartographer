package org.commonjava.maven.cartographer.preset;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;

public interface PresetFactory
{

    String getPresetId();

    ProjectRelationshipFilter newFilter( GraphWorkspace workspace );

}
