package org.commonjava.maven.cartographer.preset;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;

@Named( "runtime" )
@ApplicationScoped
public class MavenRuntimeFilterFactory
    implements PresetFactory
{
    @Override
    public ProjectRelationshipFilter newFilter( final GraphWorkspace workspace )
    {
        return new MavenRuntimeFilter();
    }

    @Override
    public String getPresetId()
    {
        return "runtime";
    }
}