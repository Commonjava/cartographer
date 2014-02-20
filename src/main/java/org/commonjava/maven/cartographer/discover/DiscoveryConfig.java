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
import java.util.Collections;
import java.util.Set;

import org.commonjava.maven.atlas.graph.mutate.GraphMutator;

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
    };

    boolean isEnabled();

    long getTimeoutMillis();

    URI getDiscoverySource();

    Set<String> getEnabledPatchers();

    GraphMutator getMutator();

}
