package org.commonjava.maven.cartographer.meta;

import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

public abstract class AbstractMetadataScanner
    implements MetadataScanner
{

    protected final Logger logger = new Logger( getClass() );

    @Inject
    private MavenPomReader pomReader;

    protected AbstractMetadataScanner()
    {
    }

    protected AbstractMetadataScanner( final MavenPomReader pomReader )
    {
        this.pomReader = pomReader;
    }

    @Override
    public Map<String, String> scan( final ProjectVersionRef ref, final List<? extends Location> locations, final Map<String, Object> context )
    {
        MavenPomView pomView = (MavenPomView) context.get( POM_VIEW_CTX_KEY );
        if ( pomView == null )
        {
            try
            {
                final Transfer transfer = (Transfer) context.get( TRANSFER_CTX_KEY );
                if ( transfer != null )
                {
                    pomView = pomReader.read( transfer, locations );
                }
                else
                {
                    pomView = pomReader.read( ref, locations );
                }

                context.put( POM_VIEW_CTX_KEY, pomView );
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( "Failed to parse: %s. Reason: %s", e, ref, e.getMessage() );
                return null;
            }
        }

        return scan( ref, pomView );
    }

    protected Map<String, String> scan( final ProjectVersionRef ref, final MavenPomView pomView )
    {
        final Map<String, String> xpaths = getMetadataKeyXPathMappings();
        final Map<String, String> metadata = new HashMap<>( xpaths.size() );
        for ( final Map.Entry<String, String> entry : xpaths.entrySet() )
        {
            final String key = entry.getKey();
            final String xpath = entry.getValue();

            try
            {
                final String scmConnection = pomView.resolveXPathExpression( xpath, false );
                if ( scmConnection != null )
                {
                    metadata.put( key, scmConnection );
                }
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( "Failed to resolve SCM element via XPath '%s' in: %s. Reason: %s", e, xpath, ref, e.getMessage() );
            }
        }

        return metadata;
    }

    protected Map<String, String> getMetadataKeyXPathMappings()
    {
        return Collections.emptyMap();
    }

}
