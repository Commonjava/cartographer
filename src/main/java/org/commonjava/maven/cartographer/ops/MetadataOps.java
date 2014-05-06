/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.gavs;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.maven.cartographer.dto.MetadataCollation;
import org.commonjava.maven.cartographer.dto.MetadataCollationRecipe;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MetadataOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected ArtifactManager artifacts;

    @Inject
    protected MavenPomReader pomReader;

    @Inject
    protected MetadataScannerSupport scannerSupport;

    @Inject
    protected DiscoverySourceManager sourceManager;

    @Inject
    protected ResolveOps resolver;

    @Inject
    protected CalculationOps calculations;

    @Inject
    protected RelationshipGraphFactory graphFactory;

    protected MetadataOps()
    {
    }

    public MetadataOps( final ArtifactManager artifacts, final MavenPomReader pomReader,
                        final MetadataScannerSupport scannerSupport, final DiscoverySourceManager sourceManager, final ResolveOps resolver,
 final CalculationOps calculations,
                        final RelationshipGraphFactory graphFactory )
    {
        this.artifacts = artifacts;
        this.pomReader = pomReader;
        this.scannerSupport = scannerSupport;
        this.sourceManager = sourceManager;
        this.resolver = resolver;
        this.calculations = calculations;
        this.graphFactory = graphFactory;
    }

    public Map<String, String> getMetadata( final String workspaceId, final ProjectVersionRef ref )
        throws CartoDataException
    {
        RelationshipGraph graph;
        try
        {
            graph = graphFactory.open( new ViewParams( workspaceId, ref ), false );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Cannot open graph for: {} in workspace: {}. Reason: {}", e, ref,
                                          workspaceId, e.getMessage() );
        }

        return graph.getMetadata( ref );
    }

    public String getMetadataValue( final String workspaceId, final ProjectVersionRef ref, final String key )
        throws CartoDataException
    {
        RelationshipGraph graph;
        try
        {
            graph = graphFactory.open( new ViewParams( workspaceId, ref ), false );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Cannot open graph for: {} in workspace: {}. Reason: {}", e, ref,
                                          workspaceId, e.getMessage() );
        }

        final Map<String, String> metadata = graph.getMetadata( ref, Collections.singleton( key ) );

        if ( metadata != null )
        {
            return metadata.get( key );
        }

        return null;
    }

    public void updateMetadata( final String workspaceId, final ProjectVersionRef ref,
                                final Map<String, String> metadata )
        throws CartoDataException
    {
        if ( metadata != null && !metadata.isEmpty() )
        {
            RelationshipGraph graph;
            try
            {
                graph = graphFactory.open( new ViewParams( workspaceId, ref ), false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Cannot open graph for: {} in workspace: {}. Reason: {}", e, ref,
                                              workspaceId, e.getMessage() );
            }

            logger.info( "Adding metadata for: {}\n\n  ", ref, new JoinString( "\n  ", metadata.entrySet() ) );

            try
            {
                graph.addMetadata( ref, metadata );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Failed to update metadata on: {} in workspace: {}. Reason: {}", e, ref,
                                              workspaceId, e.getMessage() );
            }
        }
    }

    // TODO: Is it important to allow a specific mutator here??
    public void rescanMetadata( final RelationshipGraph graph )
    {
        final Set<URI> sources = graph.getSources();
        final List<? extends Location> locations = sourceManager.createLocations( sources );

        for ( final ProjectVersionRef ref : graph.getAllProjects() )
        {
            Transfer transfer;
            MavenPomView pomView;
            try
            {
                transfer = artifacts.retrieveFirst( locations, ref.asPomArtifact() );
                if ( transfer == null )
                {
                    logger.error( "Cannot find POM: {} in locations: {}. Skipping for metadata scanning...", ref.asPomArtifact(), locations );
                }

                pomView = pomReader.read( ref, transfer, locations );
            }
            catch ( final TransferException e )
            {
                logger.error( String.format( "Cannot read: %s from locations: %s. Reason: %s", ref.asPomArtifact(), locations, e.getMessage() ), e );
                continue;
            }
            catch ( final GalleyMavenException e )
            {
                logger.error( String.format( "Cannot build POM view for: %s. Reason: %s", ref.asPomArtifact(), e.getMessage() ), e );
                continue;
            }

            final Map<String, String> allMeta = scannerSupport.scan( ref, locations, pomView, transfer );

            if ( allMeta != null && !allMeta.isEmpty() )
            {
                try
                {
                    graph.addMetadata( ref, allMeta );
                }
                catch ( final RelationshipGraphException e )
                {
                    logger.error( String.format( "Failed to update metadata for: %s in: %s. Reason: %s", ref, graph,
                                                 e.getMessage() ), e );
                }
            }
        }
    }

    public MetadataCollation collate( final MetadataCollationRecipe recipe )
        throws CartoDataException
    {
        resolver.resolve( recipe );

        final GraphComposition graphs = recipe.getGraphComposition();

        Set<ProjectVersionRef> gavs;
        RelationshipGraph graph;
        if ( graphs.getCalculation() != null && graphs.size() > 1 )
        {
            try
            {
                graph =
                    graphFactory.open( new ViewParams( recipe.getWorkspaceId(), AnyFilter.INSTANCE,
                                                       new ManagedDependencyMutator() ), false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Cannot open root-less graph in workspace: {}. Reason: {}", e,
                                              recipe.getWorkspaceId(), e.getMessage() );
            }

            final GraphCalculation result = calculations.calculate( recipe.getWorkspaceId(), graphs );
            gavs = gavs( result.getResult() );
        }
        else
        {
            final GraphDescription graphDesc = graphs.getGraphs()
                                                     .get( 0 );

            final ProjectVersionRef[] roots = graphDesc.getRootsArray();
            try
            {
                graph =
                    graphFactory.open( new ViewParams( recipe.getWorkspaceId(), graphDesc.getFilter(),
                                                       new ManagedDependencyMutator(), roots ), false );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Cannot open graph for: {} in workspace: {}. Reason: {}", e,
                                              new JoinString( ", ", roots ), recipe.getWorkspaceId(), e.getMessage() );
            }

            gavs = graph.getAllProjects();
        }

        final Map<Map<String, String>, Set<ProjectVersionRef>> map = graph.collateByMetadata( gavs, recipe.getKeys() );

        for ( final Map<String, String> metadata : new HashSet<Map<String, String>>( map.keySet() ) )
        {
            final Map<String, String> changed = metadata == null ? new HashMap<String, String>() : new HashMap<String, String>( metadata );
            for ( final String key : recipe.getKeys() )
            {
                if ( !changed.containsKey( key ) )
                {
                    changed.put( key, null );
                }
            }

            // long way around to preserve Map.equals() on the overall collation, 
            // since changing a key of type Map leaves equals() of the containing 
            // Map undefined.
            final Set<ProjectVersionRef> refs = map.remove( metadata );
            map.put( changed, refs );
        }

        return new MetadataCollation( map );
    }
}
