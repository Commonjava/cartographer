package org.commonjava.maven.cartographer.ops;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.util.logging.Logger;

public class MetadataOps
{

    private final Logger logger = new Logger( getClass() );

    private final CartoDataManager data;

    public MetadataOps( final CartoDataManager data )
    {
        this.data = data;
    }

    public Map<String, String> getMetadata( final ProjectVersionRef ref )
        throws CartoDataException
    {
        return data.getMetadata( ref );
    }

    public String getMetadataValue( final ProjectVersionRef ref, final String key )
        throws CartoDataException
    {
        final Map<String, String> metadata = data.getMetadata( ref );

        if ( metadata != null )
        {
            return metadata.get( key );
        }

        return null;
    }

    public void updateMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        if ( metadata != null && !metadata.isEmpty() )
        {
            logger.info( "Adding metadata for: %s\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );

            data.addMetadata( ref, metadata );
        }
    }
}
