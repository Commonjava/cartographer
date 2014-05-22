/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.dto;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.galley.model.Location;

public class ResolverRecipe
{

    protected GraphComposition graphComposition;

    protected String workspaceId;

    protected List<String> patcherIds;

    protected Integer timeoutSecs;

    protected boolean resolve;

    protected transient Location sourceLocation;

    public String getWorkspaceId()
    {
        return workspaceId;
    }

    public Location getSourceLocation()
    {
        return sourceLocation;
    }

    public void setWorkspaceId( final String workspaceId )
    {
        this.workspaceId = workspaceId;
    }

    public void setSourceLocation( final Location source )
    {
        this.sourceLocation = source;
    }

    public DiscoveryConfig getDiscoveryConfig()
        throws URISyntaxException
    {
        final DefaultDiscoveryConfig ddc = new DefaultDiscoveryConfig( getSourceLocation().getUri() );
        ddc.setEnabled( true );

        return ddc;
    }

    public Integer getTimeoutSecs()
    {
        return timeoutSecs == null ? 10 : timeoutSecs;
    }

    public void setTimeoutSecs( final Integer timeoutSecs )
    {
        this.timeoutSecs = timeoutSecs;
    }

    public List<String> getPatcherIds()
    {
        return patcherIds;
    }

    public void setPatcherIds( final Collection<String> patcherIds )
    {
        this.patcherIds = new ArrayList<>();
        for ( final String id : patcherIds )
        {
            if ( !this.patcherIds.contains( id ) )
            {
                patcherIds.add( id );
            }
        }
    }

    public GraphComposition getGraphComposition()
    {
        return graphComposition;
    }

    public void setGraphComposition( final GraphComposition graphComposition )
    {
        this.graphComposition = graphComposition;
    }

    public void resolveFilters( final PresetSelector presets, final String defaultPreset )
    {
        graphComposition.resolveFilters( presets, defaultPreset );
    }

    public ResolverRecipe()
    {
        super();
    }

    public boolean isResolve()
    {
        return resolve;
    }

    public void setResolve( final boolean resolve )
    {
        this.resolve = resolve;
    }

    public boolean isValid()
    {
        return getWorkspaceId() != null && getSourceLocation() != null && graphComposition != null
            && graphComposition.isValid();
    }

    public void normalize()
    {
        graphComposition.normalize();
        normalize( patcherIds );
    }

    protected void normalize( final Collection<?> coll )
    {
        if ( coll == null )
        {
            return;
        }

        for ( final Iterator<?> it = coll.iterator(); it.hasNext(); )
        {
            if ( it.next() == null )
            {
                it.remove();
            }
        }
    }
}
