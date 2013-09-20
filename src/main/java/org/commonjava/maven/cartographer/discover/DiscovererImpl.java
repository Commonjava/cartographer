package org.commonjava.maven.cartographer.discover;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.patch.PatcherSupport;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.reader.MavenPomReader;
import org.commonjava.maven.galley.maven.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;

@ApplicationScoped
@Named( "default-carto-discoverer" )
public class DiscovererImpl
    implements ProjectRelationshipDiscoverer
{

    @Inject
    private ArtifactManager artifactManager;

    @Inject
    private MavenModelProcessor modelProcessor;

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private PatcherSupport patchers;

    @Inject
    private MavenPomReader pomReader;

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

        final List<? extends Location> locations = Arrays.asList( location );

        final MavenPomView pomView = pomReader.read( transfer, locations );

        DiscoveryResult result = null;
        if ( pomView != null )
        {
            result = modelProcessor.readRelationships( pomView, discoveryConfig.getDiscoverySource() );
        }

        if ( result != null )
        {
            result = patchers.patch( result, discoveryConfig.getEnabledPatchers(), locations, pomView, transfer );

            if ( storeRelationships )
            {
                final Set<ProjectRelationship<?>> rejected = dataManager.storeRelationships( result.getAcceptedRelationships() );
                result = new DiscoveryResult( result.getSource(), result, rejected );
            }
        }

        return result;
    }

}
