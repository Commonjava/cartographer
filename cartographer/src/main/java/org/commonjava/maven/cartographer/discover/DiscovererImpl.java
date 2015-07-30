/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.cartographer.discover;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
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
    private PatcherSupport patchers;

    @Inject
    private MetadataScannerSupport metadataScanners;

    @Inject
    private MavenPomReader pomReader;

    protected DiscovererImpl()
    {
    }

    public DiscovererImpl( final MavenModelProcessor modelProcessor, final MavenPomReader pomReader,
                           final ArtifactManager artifactManager, final PatcherSupport patchers,
                           final MetadataScannerSupport metadataScanners )
    {
        this.modelProcessor = modelProcessor;
        this.pomReader = pomReader;
        this.artifactManager = artifactManager;
        this.patchers = patchers;
        this.metadataScanners = metadataScanners;
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final List<? extends Location> locations = discoveryConfig.getLocations();

        try
        {
            return artifactManager.resolveVariableVersion( locations, ref );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve variable version for: {}. Reason: {}", e, ref,
                                          e.getMessage() );
        }
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final RelationshipGraph graph,
                                                  final DiscoveryConfig discoveryConfig )
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

        final List<? extends Location> locations = discoveryConfig.getLocations();

        Transfer transfer;
        final MavenPomView pomView;
        try
        {
            transfer = artifactManager.retrieveFirst( locations, specific.asPomArtifact() );
            if ( transfer == null )
            {
                return null;
            }

            pomView = pomReader.read( specific, transfer, locations );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to retrieve POM: {} from: {}. Reason: {}", e, specific, locations,
                                          e.getMessage() );
        }
        catch ( final GalleyMavenException e )
        {
            throw new CartoDataException( "Failed to parse POM: {} from: {}. Reason: {}", e, specific, locations,
                                          e.getMessage() );
        }

        DiscoveryResult result = null;
        if ( pomView != null )
        {
            result = modelProcessor.readRelationships( pomView, discoveryConfig.getDiscoverySource(), discoveryConfig );
        }

        if ( result != null )
        {
            result = patchers.patch( result, discoveryConfig.getEnabledPatchers(), locations, pomView, transfer );

            final Map<String, String> metadata =
                metadataScanners.scan( result.getSelectedRef(), locations, pomView, transfer );
            result.setMetadata( metadata );

            if ( discoveryConfig.isStoreRelationships() )
            {
                final Set<ProjectRelationship<?>> rejected;
                try
                {
                    rejected = graph.storeRelationships( result.getAcceptedRelationships() );
                    graph.addMetadata( result.getSelectedRef(), metadata );
                }
                catch ( final RelationshipGraphException e )
                {
                    throw new CartoDataException( "Failed to store relationships or metadata for: {}. Reason: {}", e,
                                                  result.getSelectedRef(), e.getMessage() );
                }

                result = new DiscoveryResult( result.getSource(), result, rejected );
            }
        }

        return result;
    }

}
