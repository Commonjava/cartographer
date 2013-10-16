package org.commonjava.maven.cartographer.discover.post.meta;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.galley.maven.parse.MavenPomReader;

@ApplicationScoped
public class ScmUrlScanner
    extends AbstractMetadataScanner
    implements MetadataScanner
{

    protected ScmUrlScanner()
    {
    }

    public ScmUrlScanner( final MavenPomReader pomReader )
    {
        super( pomReader );
    }

    private static final Map<String, String> KEYS_TO_PATHS = new HashMap<String, String>()
    {
        {
            put( "scm-connection", "/project/scm/connection/text()" );
            put( "scm-url", "/project/scm/url/text()" );
        }

        private static final long serialVersionUID = 1L;
    };

    @Override
    protected Map<String, String> getMetadataKeyXPathMappings()
    {
        return KEYS_TO_PATHS;
    }
}
