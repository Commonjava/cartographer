package org.commonjava.maven.cartographer.discover;

import java.net.URI;

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
    };

    boolean isEnabled();

    long getTimeoutMillis();

    URI getDiscoverySource();

}
