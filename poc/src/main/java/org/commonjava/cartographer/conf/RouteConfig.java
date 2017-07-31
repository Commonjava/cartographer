package org.commonjava.cartographer.conf;

/**
 * Configures routing elements, so we can switch from one bus to another for production vs. localhost deployments
 */
public class RouteConfig
{
    private String getTopicRoutePrefix;

    public String getGetTopicRoutePrefix()
    {
        return getTopicRoutePrefix;
    }

    public void setGetTopicRoutePrefix( final String getTopicRoutePrefix )
    {
        this.getTopicRoutePrefix = getTopicRoutePrefix;
    }
}
