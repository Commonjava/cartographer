/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.request;

import java.util.*;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;


public class GraphDescription
{

    private Set<ProjectVersionRef> roots;

    private String preset;
    
    private String mutator;
    
    private GraphMutator mutatorInstance;

    private Map<String, Object> presetParams;

    private transient ProjectRelationshipFilter filter;

    protected transient String defaultPreset;

    protected GraphDescription()
    {
    }

    public GraphDescription( final String preset, final String mutator,
                             final Map<String, ?> presetParams, final Collection<ProjectVersionRef> roots )
    {
        this.preset = preset;
        this.mutator = mutator;
        this.presetParams = presetParams == null ? new TreeMap<>() : new TreeMap<>( presetParams );
        this.roots = new TreeSet<ProjectVersionRef>( roots );
    }

    public GraphDescription( final String preset, final String mutator, final Map<String, ?> presetParams,
                             final ProjectVersionRef... roots )
    {
        this( preset, mutator, presetParams, Arrays.asList( roots ) );
    }

    public GraphDescription( final ProjectRelationshipFilter filter, final String mutator,
                             final Collection<ProjectVersionRef> roots )
    {
        this.filter = filter;
        this.mutator = mutator;
        this.roots = new TreeSet<ProjectVersionRef>( roots );
    }

    public GraphDescription( final ProjectRelationshipFilter filter, final String mutator,
                             final ProjectVersionRef... roots )
    {
        this( filter, mutator, Arrays.asList( roots ) );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public String getPreset()
    {
        return preset;
    }

    public String getMutator()
    {
        return mutator;
    }

    public void setRoots( final Set<ProjectVersionRef> roots )
    {
        this.roots = new TreeSet<>( roots );
    }

    public void setPreset( final String preset )
    {
        this.preset = preset;
    }

    public void setMutator( final String mutator )
    {
        this.mutator = mutator;
    }

    public ProjectVersionRef[] rootsArray()
    {
        return roots == null ? new ProjectVersionRef[0] : roots.toArray( new ProjectVersionRef[roots.size()] );
    }

    public ProjectRelationshipFilter filter()
    {
        return filter;
    }

    public void setFilter( final ProjectRelationshipFilter filter )
    {
        this.filter = filter;
    }

    public GraphMutator getMutatorInstance()
    {
        return mutatorInstance;
    }
    
    public void setMutatorInstance( final GraphMutator mutatorInstance )
    {
        this.mutatorInstance = mutatorInstance;
    }

    @Override
    public String toString()
    {
        return String.format( "GraphDescription [roots=%s, preset=%s, mutator=%s, filter=%s, presetParams=%s]", roots,
                              preset, mutator, filter, presetParams );
    }

    public void normalize()
    {
        for ( final Iterator<ProjectVersionRef> it = roots.iterator(); it.hasNext(); )
        {
            if ( it.next() == null )
            {
                it.remove();
            }
        }
    }

    public Map<String, Object> getPresetParams()
    {
        return presetParams == null ? new HashMap<>() : presetParams;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( preset == null ) ? 0 : preset.hashCode() );
        result = prime * result + ( ( mutator == null ) ? 0 : mutator.hashCode() );
        result = prime * result + ( ( presetParams == null ) ? 0 : presetParams.hashCode() );
        result = prime * result + ( ( roots == null ) ? 0 : roots.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final GraphDescription other = (GraphDescription) obj;
        if ( filter == null )
        {
            if ( other.filter != null )
            {
                return false;
            }
        }
        else if ( !filter.equals( other.filter ) )
        {
            return false;
        }
        if ( preset == null )
        {
            if ( other.preset != null )
            {
                return false;
            }
        }
        else if ( !preset.equals( other.preset ) )
        {
            return false;
        }
        if ( mutator == null )
        {
            if ( other.mutator != null )
            {
                return false;
            }
        }
        else if ( !mutator.equals( other.mutator ) )
        {
            return false;
        }

        Map<String, Object> presetParams = getPresetParams();
        Map<String, Object> otherPresetParams = other.getPresetParams();
        if ( presetParams == null )
        {
            if ( otherPresetParams != null )
            {
                return false;
            }
        }
        else if ( !presetParams.equals( otherPresetParams ) )
        {
            return false;
        }
        if ( roots == null )
        {
            if ( other.roots != null )
            {
                return false;
            }
        }
        else if ( !roots.equals( other.roots ) )
        {
            return false;
        }
        return true;
    }

    public String getDefaultPreset()
    {
        return defaultPreset;
    }

    public void setDefaultPreset( final String defaultPreset )
    {
        this.defaultPreset = defaultPreset;
    }

}
