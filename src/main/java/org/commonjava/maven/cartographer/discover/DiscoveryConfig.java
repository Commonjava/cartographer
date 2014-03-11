/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

    Set<String> getEnabledPatchers();

    GraphMutator getMutator();

    List<? extends Location> getLocations();

    void setLocations( Collection<? extends Location> locations );

    boolean isStoreRelationships();

    void setStoreRelationships( boolean store );

    boolean isIncludeBuildSection();

    boolean isIncludeManagedDependencies();

    boolean isIncludeManagedPlugins();

}
