/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.cartographer.discover;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
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
    private MetadataScannerSupport metadataScanners;

    @Inject
    private MavenPomReader pomReader;

    protected DiscovererImpl()
    {
    }

    public DiscovererImpl( final MavenModelProcessor modelProcessor, final MavenPomReader pomReader, final ArtifactManager artifactManager,
                           final CartoDataManager dataManager, final PatcherSupport patchers, final MetadataScannerSupport metadataScanners )
    {
        this.modelProcessor = modelProcessor;
        this.pomReader = pomReader;
        this.artifactManager = artifactManager;
        this.dataManager = dataManager;
        this.patchers = patchers;
        this.metadataScanners = metadataScanners;
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
            throw new CartoDataException( "Failed to resolve variable version for: {}. Reason: {}", e, ref, e.getMessage() );
        }
    }

    /**
     * @deprecated Use {@link #discoverRelationships(ProjectVersionRef,DiscoveryConfig)} instead
     */
    @Deprecated
    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig, final boolean storeRelationships )
        throws CartoDataException
    {
        discoveryConfig.setStoreRelationships( storeRelationships );
        return discoverRelationships( ref, discoveryConfig );
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        ProjectVersionRef specific = ref;
        if ( !ref.isSpecificVersion() )
        {
            specific = resolveSpecificVersion( ref, discoveryConfig );
        }

        if ( specific == null )
        {
            specific = ref;
        }

        final Location location = new SimpleLocation( discoveryConfig.getDiscoverySource()
                                                                     .toString() );

        final List<? extends Location> locations = Arrays.asList( location );

        Transfer transfer;
        final MavenPomView pomView;
        try
        {
            transfer = artifactManager.retrieve( location, specific.asPomArtifact() );
            if ( transfer == null )
            {
                return null;
            }

            pomView = pomReader.read( specific, transfer, locations );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to retrieve POM: {} from: {}. Reason: {}", e, specific, location, e.getMessage() );
        }
        catch ( final GalleyMavenException e )
        {
            throw new CartoDataException( "Failed to parse POM: {} from: {}. Reason: {}", e, specific, location, e.getMessage() );
        }

        DiscoveryResult result = null;
        if ( pomView != null )
        {
            result = modelProcessor.readRelationships( pomView, discoveryConfig.getDiscoverySource(), discoveryConfig );
        }

        if ( result != null )
        {
            result = patchers.patch( result, discoveryConfig.getEnabledPatchers(), locations, pomView, transfer );

            final Map<String, String> metadata = metadataScanners.scan( result.getSelectedRef(), locations, pomView, transfer );
            result.setMetadata( metadata );

            if ( discoveryConfig.isStoreRelationships() )
            {
                final Set<ProjectRelationship<?>> rejected = dataManager.storeRelationships( result.getAcceptedRelationships() );
                dataManager.addMetadata( result.getSelectedRef(), metadata );

                result = new DiscoveryResult( result.getSource(), result, rejected );
            }
        }

        return result;
    }

}
