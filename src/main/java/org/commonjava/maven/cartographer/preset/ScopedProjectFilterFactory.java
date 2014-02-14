package org.commonjava.maven.cartographer.preset;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.atservice.annotation.Service;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;

@Named( "scope" )
@ApplicationScoped
@Service( PresetFactory.class )
public class ScopedProjectFilterFactory
    implements PresetFactory
{

    private static final String[] IDS = { "runtime", "test", "provided", "compile", "scope" };

    @Override
    public String[] getPresetIds()
    {
        return IDS;
    }

    @Override
    public ProjectRelationshipFilter newFilter( final String presetId, final GraphWorkspace workspace, final Map<String, Object> parameters )
    {
        return null;
    }

}
