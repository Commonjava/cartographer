package org.commonjava.cartographer.rest.util;

import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

import static org.apache.commons.lang.StringUtils.join;

/**
 * Created by jdcasey on 4/18/16.
 */
public class RestUtils
{
    public static String formatUrlTo( String base, String... parts )
    {
        Logger logger = LoggerFactory.getLogger( RestUtils.class );
        logger.debug( "Formatting URL from base: '{}' and parts: {}", base, new JoinString( ", ", parts ) );

        String url = null;
        try
        {
            url = UrlUtils.buildUrl( base, parts );
        }
        catch ( final MalformedURLException e )
        {
            logger.warn( "Failed to use UrlUtils to build URL from base: {} and parts: {}", base, join( parts, ", " ) );
            url = PathUtils.normalize( base, PathUtils.normalize( parts ) );
        }

        if ( url.length() > 0 && !url.matches( "[a-zA-Z0-9]+\\:\\/\\/.+" ) && url.charAt( 0 ) != '/' )
        {
            url = "/" + url;
        }

        logger.debug( "Resulting URL: '{}'", url );

        return url;
    }
}
