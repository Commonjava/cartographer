package org.commonjava.maven.cartographer.discover;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.patch.PatcherSupport;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
@Named( "default-carto-discoverer" )
public class DiscovererImpl
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ArtifactManager artifactManager;

    @Inject
    private MavenModelProcessor modelProcessor;

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private PatcherSupport patchers;

    protected DiscovererImpl()
    {
    }

    public DiscovererImpl( final MavenModelProcessor modelProcessor, final ArtifactManager artifactManager, final CartoDataManager dataManager,
                           final PatcherSupport patchers )
    {
        this.modelProcessor = modelProcessor;
        this.artifactManager = artifactManager;
        this.dataManager = dataManager;
        this.patchers = patchers;
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final Location location = new SimpleLocation( discoveryConfig.getDiscoverySource()
                                                                     .toString() );

        try
        {
            return artifactManager.resolveVariableVersion( Arrays.asList( location ), ref );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve variable version for: %s. Reason: %s", e, ref, e.getMessage() );
        }
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig, final boolean storeRelationships )
        throws CartoDataException
    {
        ProjectVersionRef specific = ref;
        if ( !ref.isSpecificVersion() )
        {
            specific = resolveSpecificVersion( ref, discoveryConfig );
        }

        final Location location = new SimpleLocation( discoveryConfig.getDiscoverySource()
                                                                     .toString() );

        Transfer transfer;
        try
        {
            transfer = artifactManager.retrieve( location, specific.asPomArtifact() );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to retrieve POM: %s from: %s. Reason: %s", e, specific, location, e.getMessage() );
        }

        Model model = null;
        if ( transfer != null && transfer.exists() )
        {
            final InputSource is = new InputSource();
            is.setLocation( transfer.getFullPath() );
            is.setModelId( specific.toString() );

            InputStream stream = null;
            try
            {
                stream = transfer.openInputStream();
                model = new MavenXpp3ReaderEx().read( stream, false, is );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to read POM for: '%s' from: '%s'. Reason: %s", e, specific, transfer, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to read POM for: '%s' from: '%s'. Reason: %s", e, specific, transfer, e.getMessage() );
            }
            finally
            {
                closeQuietly( stream );
            }
        }

        DiscoveryResult result = null;
        if ( model != null )
        {
            result = modelProcessor.readRelationships( model, discoveryConfig.getDiscoverySource() );
        }

        if ( result != null )
        {
            final List<? extends Location> locations = Arrays.asList( location );

            result = patchers.patch( result, discoveryConfig.getEnabledPatchers(), locations, model, transfer );

            if ( storeRelationships )
            {
                final Set<ProjectRelationship<?>> rejected = dataManager.storeRelationships( result.getAcceptedRelationships() );
                result = new DiscoveryResult( result.getSource(), result, rejected );
            }
        }

        return result;
    }

}
