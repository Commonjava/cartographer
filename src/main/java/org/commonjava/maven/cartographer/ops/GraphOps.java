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
package org.commonjava.maven.cartographer.ops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;
import org.commonjava.maven.cartographer.dto.GraphExport;
import org.commonjava.maven.cartographer.util.ProjectVersionRefComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GraphOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected RelationshipGraphFactory graphFactory;

    protected GraphOps()
    {
    }

    public GraphOps( final RelationshipGraphFactory graphFactory )
    {
        this.graphFactory = graphFactory;
    }

    public BuildOrder getBuildOrder( final RelationshipGraph graph )
        throws CartoDataException
    {
        if ( graph != null )
        {
            final BuildOrderTraversal traversal = new BuildOrderTraversal();

            logger.info( "Performing build-order traversal for graph: {}", graph );
            try
            {
                graph.traverse( traversal );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Failed to construct build order for: {}. Reason: {}", e, graph,
                                              e.getMessage() );
            }

            return traversal.getBuildOrder();
        }

        return null;
    }

    public List<ProjectVersionRef> listProjects( final String groupIdPattern, final String artifactIdPattern,
                                                 final RelationshipGraph graph )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> all = graph.getAllProjects();
        final List<ProjectVersionRef> matching = new ArrayList<ProjectVersionRef>();
        if ( all != null )
        {
            if ( groupIdPattern != null || artifactIdPattern != null )
            {
                final String gip = groupIdPattern == null ? ".*" : groupIdPattern.replaceAll( "\\*", ".*" );
                final String aip = artifactIdPattern == null ? ".*" : artifactIdPattern.replaceAll( "\\*", ".*" );

                logger.info( "Filtering {} projects using groupId pattern: '{}' and artifactId pattern: '{}'",
                             all.size(), gip, aip );

                for ( final ProjectVersionRef ref : all )
                {
                    if ( ref.getGroupId()
                            .matches( gip ) && ref.getArtifactId()
                                                  .matches( aip ) )
                    {
                        matching.add( ref );
                    }
                }
            }
            else
            {
                logger.info( "Returning all {} projects", all.size() );
                matching.addAll( all );
            }

        }

        if ( !matching.isEmpty() )
        {
            Collections.sort( matching, new ProjectVersionRefComparator() );

        }

        return matching;
    }

    public String getProjectError( final ProjectVersionRef ref, final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            return graph.getProjectError( ref );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to query graph: {}. Reason: {}", e, params, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public Map<ProjectVersionRef, String> getAllProjectErrors( final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            return graph.getAllProjectErrors();
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to query graph: {}. Reason: {}", e, params, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public List<ProjectVersionRef> listProjects( final String groupIdPattern, final String artifactIdPattern,
                                                 final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            return listProjects( groupIdPattern, artifactIdPattern, graph );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to list graph projects: {}. Reason: {}", e, params, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public ProjectVersionRef getProjectParent( final ProjectVersionRef ref, final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            if ( !graph.containsGraph( ref ) )
            {
                return null;
            }

            final Set<ProjectRelationship<?>> rels =
                graph.findDirectRelationshipsFrom( ref, false, RelationshipType.PARENT );
            if ( rels == null || rels.isEmpty() )
            {
                return ref;
            }

            final ParentRelationship parent = (ParentRelationship) rels.iterator()
                                                                       .next();
            return parent.getTarget();
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to query graph relationships: {}. Reason: {}", e, params,
                                          e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public Set<ProjectRelationship<?>> getDirectRelationshipsFrom( final ProjectVersionRef ref,
                                                                   final ViewParams params,
                                                                   final RelationshipType... types )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            if ( !graph.containsGraph( ref ) )
            {
                return Collections.emptySet();
            }

            return graph.findDirectRelationshipsFrom( ref, false, types );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to query graph relationships: {}. Reason: {}", e, params,
                                          e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final ProjectVersionRef ref, final ViewParams params,
                                                                 final RelationshipType... types )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            if ( !graph.containsGraph( ref ) )
            {
                return Collections.emptySet();
            }

            return graph.findDirectRelationshipsTo( ref, false, types );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to query graph relationships: {}. Reason: {}", e, params,
                                          e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public void reindex( final ProjectVersionRef ref, final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            if ( !graph.containsGraph( ref ) )
            {
                return;
            }

            graph.reindex( ref );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to reindex graph relationships: {}. Reason: {}", e, params,
                                          e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public void reindexAll( final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            graph.reindex();
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to reindex graph relationships: {}. Reason: {}", e, params,
                                          e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public Set<ProjectVersionRef> getIncomplete( final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            return graph.getAllIncompleteSubgraphs();
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to get missing project references from graph: {}. Reason: {}", e,
                                          params, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public Set<ProjectVersionRef> getVariable( final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            return graph.getAllVariableSubgraphs();
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to get variable project references from graph: {}. Reason: {}", e,
                                          params, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public List<ProjectVersionRef> getAncestry( final ProjectVersionRef root, final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            return CartoGraphUtils.getAncestry( root, graph );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to get ancestry of: {} from graph: {}. Reason: {}", e, root, params,
                                          e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public BuildOrder getBuildOrder( final ProjectVersionRef ref, final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );
            return CartoGraphUtils.getBuildOrder( ref, graph );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to get build order for: {} from graph: {}. Reason: {}", e, ref,
                                          params, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public GraphExport exportGraph( final ViewParams params )
        throws CartoDataException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( params, false );

            final Set<ProjectRelationship<?>> rels = graph.getAllRelationships();
            final Set<ProjectVersionRef> missing = graph.getAllIncompleteSubgraphs();
            final Set<ProjectVersionRef> variable = graph.getAllVariableSubgraphs();
            final Map<ProjectVersionRef, String> errors = graph.getAllProjectErrors();
            return new GraphExport( rels, missing, variable, errors );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Failed to export graph: {}. Reason: {}", e, params, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

}
