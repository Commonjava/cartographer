package org.commonjava.maven.cartographer.discover;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DefaultDiscoveryConfig
    implements DiscoveryConfig
{

    private boolean enabled = true;

    private long timeoutMillis = TimeUnit.MILLISECONDS.convert( 60, TimeUnit.SECONDS );

    private final URI discoverySource;

    private Set<String> patchers;

    public DefaultDiscoveryConfig( final URI discoverySource )
    {
        this.discoverySource = discoverySource;
    }

    public DefaultDiscoveryConfig( final String discoverySource )
        throws URISyntaxException
    {
        this.discoverySource = new URI( discoverySource );
    }

    public DefaultDiscoveryConfig setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
        return this;
    }

    public DefaultDiscoveryConfig setTimeoutMillis( final long millis )
    {
        this.timeoutMillis = millis;
        return this;
    }

    public DefaultDiscoveryConfig setEnabledPatchers( final Set<String> patchers )
    {
        this.patchers = patchers;
        return this;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public long getTimeoutMillis()
    {
        return timeoutMillis;
    }

    @Override
    public URI getDiscoverySource()
    {
        return discoverySource;
    }

    @Override
    public Set<String> getEnabledPatchers()
    {
        return patchers == null ? Collections.<String> emptySet() : patchers;
    }

}
