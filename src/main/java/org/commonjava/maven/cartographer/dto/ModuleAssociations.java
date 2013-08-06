package org.commonjava.maven.cartographer.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ModuleAssociations
{

    private ProjectVersionRef root;

    private Set<ProjectVersionRef> modules;

    public ModuleAssociations()
    {
    }

    public ModuleAssociations( final ProjectVersionRef root )
    {
        this.root = root;
        this.modules = new HashSet<ProjectVersionRef>();
    }

    public ModuleAssociations( final ProjectVersionRef root, final Collection<ProjectVersionRef> modules )
    {
        this.root = root;
        this.modules = new HashSet<ProjectVersionRef>( modules );
    }

    public void addModule( final ProjectVersionRef module )
    {
        modules.add( module );
    }

    public ProjectVersionRef getRoot()
    {
        return root;
    }

    public Set<ProjectVersionRef> getModules()
    {
        return modules;
    }

    public void setRoot( final ProjectVersionRef root )
    {
        this.root = root;
    }

    public void setModules( final Set<ProjectVersionRef> modules )
    {
        this.modules = modules;
    }

}
