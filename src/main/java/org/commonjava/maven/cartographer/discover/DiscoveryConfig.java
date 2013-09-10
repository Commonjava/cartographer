package org.commonjava.maven.cartographer.discover;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

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
    };

    boolean isEnabled();

    long getTimeoutMillis();

    URI getDiscoverySource();

    Set<String> getEnabledPatchers();

}
