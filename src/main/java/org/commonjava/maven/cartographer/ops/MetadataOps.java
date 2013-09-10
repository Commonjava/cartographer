package org.commonjava.maven.cartographer.ops;

import static org.apache.commons.lang.StringUtils.join;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.meta.MetadataScanner;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class MetadataOps
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager data;

    private Instance<MetadataScanner> scannerInstances;

    private Set<MetadataScanner> metadataScanners;

    protected MetadataOps()
    {
    }

    public MetadataOps( final CartoDataManager data, final Set<MetadataScanner> metadataScanners )
    {
        this.data = data;
        this.metadataScanners = metadataScanners;
    }

    public void setupScannerInstances()
    {
        metadataScanners = new HashSet<>();
        for ( final MetadataScanner scanner : scannerInstances )
        {
            metadataScanners.add( scanner );
        }
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

    public void rescanMetadata( final ProjectRelationshipFilter filter, final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final Map<String, Object> scannerCache = new HashMap<>();

        final EProjectWeb web = data.getProjectWeb( filter, roots );
        for ( final ProjectVersionRef ref : web.getAllProjects() )
        {
            final Map<String, String> allMeta = new HashMap<>();
            for ( final MetadataScanner scanner : metadataScanners )
            {
                final Map<String, String> result = scanner.scan( ref, scannerCache );
                if ( result != null )
                {
                    allMeta.putAll( result );
                }
            }

            if ( !allMeta.isEmpty() )
            {
                data.addMetadata( ref, allMeta );
            }
        }
    }
}
