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
package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.gavs;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
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
    private CartoDataManager data;

    @Inject
    private ArtifactManager artifacts;

    @Inject
    private MavenPomReader pomReader;

    @Inject
    private MetadataScannerSupport scannerSupport;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private ResolveOps resolver;

    @Inject
    private CalculationOps calculations;

    protected MetadataOps()
    {
    }

    public MetadataOps( final CartoDataManager data, final ArtifactManager artifacts, final MavenPomReader pomReader,
                        final MetadataScannerSupport scannerSupport, final DiscoverySourceManager sourceManager, final ResolveOps resolver,
                        final CalculationOps calculations )
    {
        this.data = data;
        this.artifacts = artifacts;
        this.pomReader = pomReader;
        this.scannerSupport = scannerSupport;
        this.sourceManager = sourceManager;
        this.resolver = resolver;
        this.calculations = calculations;
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
            logger.info( "Adding metadata for: {}\n\n  ", ref, new JoinString( "\n  ", metadata.entrySet() ) );

            data.addMetadata( ref, metadata );
        }
    }

    public void rescanMetadata( final ProjectRelationshipFilter filter, final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final EProjectNet web = data.getProjectWeb( filter, roots );
        final Set<URI> sources = web.getSources();
        final List<? extends Location> locations = sourceManager.createLocations( sources );

        for ( final ProjectVersionRef ref : web.getAllProjects() )
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
                data.addMetadata( ref, allMeta );
            }
        }
    }

    public MetadataCollation collate( final MetadataCollationRecipe recipe )
        throws CartoDataException
    {
        data.setCurrentWorkspace( recipe.getWorkspaceId() );
        resolver.resolve( recipe );

        final GraphComposition graphs = recipe.getGraphComposition();

        Set<ProjectVersionRef> gavs;
        if ( graphs.getCalculation() != null && graphs.size() > 1 )
        {
            final GraphCalculation result = calculations.calculate( graphs );
            gavs = gavs( result.getResult() );
        }
        else
        {
            final GraphDescription graphDesc = graphs.getGraphs()
                                                     .get( 0 );

            final ProjectVersionRef[] roots = graphDesc.getRootsArray();
            final EProjectWeb web = data.getProjectWeb( graphDesc.getFilter(), roots );

            if ( web == null )
            {
                throw new CartoDataException( "Failed to retrieve web for roots: {}", new JoinString( ", ", roots ) );
            }

            gavs = web.getAllProjects();
        }

        final Map<Map<String, String>, Set<ProjectVersionRef>> map = data.collateProjectsByMetadata( gavs, recipe.getKeys() );

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
