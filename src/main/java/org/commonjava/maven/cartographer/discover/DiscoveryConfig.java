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
package org.commonjava.maven.cartographer.discover;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.galley.model.Location;

public interface DiscoveryConfig
{

    DiscoveryConfig DISABLED = new DiscoveryConfig()
    {
        @Override
        public boolean isEnabled()
        {
            return false;
        }

        @Override
        public long getTimeoutMillis()
        {
            return 0;
        }

        @Override
        public URI getDiscoverySource()
        {
            return null;
        }

        @Override
        public Set<String> getEnabledPatchers()
        {
            return Collections.emptySet();
        }

        @Override
        public GraphMutator getMutator()
        {
            return null;
        }

        @Override
        public List<? extends Location> getLocations()
        {
            return null;
        }

        @Override
        public void setLocations( final Collection<? extends Location> locations )
        {
        }

        @Override
        public boolean isStoreRelationships()
        {
            return false;
        }

        @Override
        public void setStoreRelationships( final boolean store )
        {
        }

        @Override
        public boolean isIncludeBuildSection()
        {
            return false;
        }

        @Override
        public boolean isIncludeManagedDependencies()
        {
            return false;
        }

        @Override
        public boolean isIncludeManagedPlugins()
        {
            return false;
        }
    };

    boolean isEnabled();

    long getTimeoutMillis();

    URI getDiscoverySource();

    Collection<String> getEnabledPatchers();

    GraphMutator getMutator();

    List<? extends Location> getLocations();

    void setLocations( Collection<? extends Location> locations );

    boolean isStoreRelationships();

    void setStoreRelationships( boolean store );

    boolean isIncludeBuildSection();

    boolean isIncludeManagedDependencies();

    boolean isIncludeManagedPlugins();

}
