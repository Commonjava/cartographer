/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.maven.cartographer.data;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.graph.workspace.AbstractGraphWorkspaceListener;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.event.CartoEventManager;
import org.commonjava.maven.cartographer.event.ErrorKey;
import org.commonjava.maven.cartographer.event.ProjectRelationshipsErrorEvent;
import org.commonjava.maven.cartographer.event.RelationshipStorageEvent;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class DefaultCartoDataManager
    extends AbstractGraphWorkspaceListener
    implements CartoDataManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private EGraphManager graphs;

    @Inject
    private GraphWorkspaceHolder workspaceHolder;

    @Inject
    private CartoEventManager funnel;

    protected DefaultCartoDataManager()
    {
    }

    public DefaultCartoDataManager( final EGraphManager graphs, final GraphWorkspaceHolder workspaceHolder, final CartoEventManager funnel )
    {
        this.graphs = graphs;
        this.workspaceHolder = workspaceHolder;
        this.funnel = funnel;
    }

    @Override
    public Set<ProjectRelationship<?>> storeRelationships( final ProjectRelationship<?>... relationships )
        throws CartoDataException
    {
        final Set<ProjectRelationship<?>> rels = graphs.storeRelationships( getCurrentWorkspace(), relationships );

        return rels;
    }

    @Override
    public Set<ProjectRelationship<?>> storeRelationships( final Collection<ProjectRelationship<?>> relationships )
        throws CartoDataException
    {
        final Set<ProjectRelationship<?>> rels = graphs.storeRelationships( getCurrentWorkspace(), relationships );
        fireStorageEvents( relationships, rels );

        return rels;
    }

    private void fireErrorEvent( final ProjectVersionRef ref, final Throwable error )
    {
        funnel.fireErrorEvent( new ProjectRelationshipsErrorEvent( new ErrorKey( ref.getGroupId(), ref.getArtifactId(), ref.getVersionString() ),
                                                                   error ) );
    }

    private void fireStorageEvents( final Collection<ProjectRelationship<?>> original, final Set<ProjectRelationship<?>> rejected )
    {
        final Set<ProjectRelationship<?>> relationships = new HashSet<>( original );
        relationships.removeAll( rejected );

        funnel.fireStorageEvent( new RelationshipStorageEvent( relationships, rejected ) );
    }

    @Override
    public synchronized EProjectGraph getProjectGraph( final ProjectVersionRef ref )
        throws CartoDataException
    {
        return graphs.getGraph( workspaceHolder.getCurrentWorkspace(), ref );
    }

    @Override
    public EProjectGraph getProjectGraph( final ProjectRelationshipFilter filter, final ProjectVersionRef discovered )
        throws CartoDataException
    {
        return graphs.getGraph( workspaceHolder.getCurrentWorkspace(), filter, discovered );
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getAncestry(org.apache.maven.graph.common.ref.ProjectVersionRef)
     */
    @Override
    public List<ProjectVersionRef> getAncestry( final ProjectVersionRef source )
        throws CartoDataException
    {
        final EProjectGraph graph = graphs.getGraph( workspaceHolder.getCurrentWorkspace(), source );
        final AncestryTraversal ancestryTraversal = new AncestryTraversal();

        try
        {
            graph.traverse( ancestryTraversal );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to traverse database for parents of: %s. Reason: %s", e, source, e.getMessage() );
        }

        return ancestryTraversal.getAncestry();
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getParent(org.apache.maven.graph.common.ref.ProjectVersionRef)
     */
    @Override
    public ProjectVersionRef getParent( final ProjectVersionRef source )
        throws CartoDataException
    {
        final Set<ProjectRelationship<?>> matches =
            graphs.findDirectRelationshipsFrom( workspaceHolder.getCurrentWorkspace(), source, false, RelationshipType.PARENT );
        if ( matches != null && !matches.isEmpty() )
        {
            final ParentRelationship parent = (ParentRelationship) matches.iterator()
                                                                          .next();
            return parent.getTarget();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getKnownChildren(org.apache.maven.graph.common.ref.ProjectVersionRef)
     */
    @Override
    public Set<ProjectVersionRef> getKnownChildren( final ProjectVersionRef parent )
        throws CartoDataException
    {
        final Set<ProjectRelationship<?>> matches =
            graphs.findDirectRelationshipsTo( workspaceHolder.getCurrentWorkspace(), parent, false, RelationshipType.PARENT );

        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : matches )
        {
            refs.add( rel.getDeclaring() );
        }

        return refs;
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getAllDirectRelationshipsWithExactSource(org.apache.maven.graph.common.ref.ProjectVersionRef, org.apache.maven.graph.common.RelationshipType)
     */
    @Override
    public Set<ProjectRelationship<?>> getAllDirectRelationshipsWithExactSource( final ProjectVersionRef source,
                                                                                 final ProjectRelationshipFilter filter )
        throws CartoDataException
    {
        final GraphView view = new GraphView( workspaceHolder.getCurrentWorkspace(), filter );
        final Set<RelationshipType> types = RelationshipUtils.getRelationshipTypes( filter );

        return graphs.findDirectRelationshipsFrom( view, source, false, types.toArray( new RelationshipType[types.size()] ) );
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getAllDirectRelationshipsWithExactTarget(org.apache.maven.graph.common.ref.ProjectVersionRef, org.apache.maven.graph.common.RelationshipType)
     */
    @Override
    public Set<ProjectRelationship<?>> getAllDirectRelationshipsWithExactTarget( final ProjectVersionRef target,
                                                                                 final ProjectRelationshipFilter filter )
        throws CartoDataException
    {
        final GraphView view = new GraphView( workspaceHolder.getCurrentWorkspace(), filter );
        final Set<RelationshipType> types = RelationshipUtils.getRelationshipTypes( filter );

        return graphs.findDirectRelationshipsTo( view, target, false, types.toArray( new RelationshipType[types.size()] ) );
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getAllDirectRelationshipsWithGASource(org.apache.maven.graph.common.ref.ProjectRef, org.apache.maven.graph.common.RelationshipType)
     */
    @Override
    public Set<ProjectRelationship<?>> getAllDirectRelationshipsWithGASource( final ProjectRef source, final ProjectRelationshipFilter filter )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> refs = getMatchingGAVs( source );
        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Set<ProjectRelationship<?>> rels = getAllDirectRelationshipsWithExactSource( ref, filter );
            if ( rels != null )
            {
                result.addAll( rels );
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getAllDirectRelationshipsWithGATarget(org.apache.maven.graph.common.ref.ProjectRef, org.apache.maven.graph.common.RelationshipType)
     */
    @Override
    public Set<ProjectRelationship<?>> getAllDirectRelationshipsWithGATarget( final ProjectRef target, final ProjectRelationshipFilter filter )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> refs = getMatchingGAVs( target );
        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Set<ProjectRelationship<?>> rels = getAllDirectRelationshipsWithExactTarget( ref, filter );
            if ( rels != null )
            {
                result.addAll( rels );
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#contains(org.apache.maven.graph.common.ref.ProjectVersionRef)
     */
    @Override
    public boolean contains( final ProjectVersionRef ref )
    {
        return graphs.containsGraph( workspaceHolder.getCurrentWorkspace(), ref );
    }

    /* (non-Javadoc)
     * @see org.commonjava.tensor.data.TensorDataManager#getAllStoredProjectRefs()
     */
    @Override
    public Set<ProjectVersionRef> getAllStoredProjectRefs()
    {
        return graphs.getAllProjects();
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        return graphs.getMetadata( getCurrentWorkspace(), ref );
    }

    @Override
    public Map<Map<String, String>, Set<ProjectVersionRef>> collateProjectsByMetadata( final Set<ProjectVersionRef> refs, final Set<String> keys )
        throws CartoDataException
    {
        return graphs.collateByMetadata( getCurrentWorkspace(), refs, keys );
    }

    @Override
    public void addMetadata( final ProjectVersionRef ref, final String name, final String value )
    {
        if ( ref == null || name == null || value == null )
        {
            return;
        }

        logger.info( "Adding metadata: '%s' = '%s' for: %s", name, value, ref );
        graphs.addMetadata( getCurrentWorkspace(), ref, name, value );
    }

    @Override
    public void addMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        if ( metadata == null )
        {
            return;
        }

        logger.info( "Adding metadata for: %s:\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );
        graphs.setMetadata( getCurrentWorkspace(), ref, metadata );
    }

    @Override
    public Set<ProjectVersionRef> getIncompleteSubgraphsFor( final ProjectVersionRef ref )
        throws CartoDataException
    {
        return graphs.getAllIncompleteSubgraphs( new GraphView( workspaceHolder.getCurrentWorkspace(), ref ) );
    }

    @Override
    public Set<ProjectVersionRef> getIncompleteSubgraphsFor( final ProjectRelationshipFilter filter, final ProjectVersionRef ref )
        throws CartoDataException
    {
        return graphs.getAllIncompleteSubgraphs( new GraphView( workspaceHolder.getCurrentWorkspace(), filter, ref ) );
    }

    @Override
    public Set<ProjectVersionRef> getAllIncompleteSubgraphs()
        throws CartoDataException
    {
        return graphs.getAllIncompleteSubgraphs( workspaceHolder.getCurrentWorkspace() );
    }

    @Override
    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final ProjectRelationshipFilter filter )
        throws CartoDataException
    {
        return graphs.getAllIncompleteSubgraphs( new GraphView( workspaceHolder.getCurrentWorkspace(), filter ) );
    }

    @Override
    public Set<ProjectVersionRef> getVariableSubgraphsFor( final ProjectVersionRef ref )
        throws CartoDataException
    {
        return graphs.getAllVariableSubgraphs( new GraphView( workspaceHolder.getCurrentWorkspace(), ref ) );
    }

    @Override
    public Set<ProjectVersionRef> getVariableSubgraphsFor( final ProjectRelationshipFilter filter, final ProjectVersionRef ref )
        throws CartoDataException
    {
        return graphs.getAllVariableSubgraphs( new GraphView( workspaceHolder.getCurrentWorkspace(), filter, ref ) );
    }

    @Override
    public Set<ProjectVersionRef> getAllVariableSubgraphs()
        throws CartoDataException
    {
        return graphs.getAllVariableSubgraphs( workspaceHolder.getCurrentWorkspace() );
    }

    @Override
    public Set<ProjectVersionRef> getAllVariableSubgraphs( final ProjectRelationshipFilter filter )
        throws CartoDataException
    {
        return graphs.getAllVariableSubgraphs( new GraphView( workspaceHolder.getCurrentWorkspace(), filter ) );
    }

    @Override
    public boolean hasErrors( final ProjectVersionRef ref )
        throws CartoDataException
    {
        final Map<String, String> md = getMetadata( ref );
        if ( md == null )
        {
            return false;
        }

        return md.containsKey( MODEL_ERRORS );
    }

    @Override
    public Set<String> getErrors( final ProjectVersionRef ref )
        throws CartoDataException
    {
        final Map<String, String> md = getMetadata( ref );
        if ( md == null )
        {
            return null;
        }

        final String serialized = md.get( MODEL_ERRORS );

        if ( isEmpty( serialized ) )
        {
            return null;
        }

        final String[] errors = serialized.split( ERROR_SEPARATOR );
        return new HashSet<String>( Arrays.asList( errors ) );
    }

    @Override
    public synchronized void clearErrors( final ProjectVersionRef ref )
    {
        final Map<String, String> md = getMetadata( ref );
        if ( md == null )
        {
            return;
        }

        final String removed = md.remove( MODEL_ERRORS );
        if ( removed != null )
        {
            graphs.setMetadata( getCurrentWorkspace(), ref, md );
        }
    }

    @Override
    public synchronized void addError( final EProjectKey key, final Throwable error )
        throws CartoDataException
    {
        final ProjectVersionRef ref = key.getProject();
        Map<String, String> md = getMetadata( ref );
        if ( md == null )
        {
            if ( !contains( ref ) )
            {
                logger.info( "No metadata for: %s. Creating disconnected project entry in database.", ref );
                graphs.addDisconnectedProject( getCurrentWorkspace(), ref );
            }

            md = getMetadata( ref );

            if ( md == null )
            {
                md = new HashMap<>();
            }
        }

        String serialized = md.get( MODEL_ERRORS );

        final String errorStr = toString( error );
        if ( isEmpty( serialized ) )
        {
            serialized = errorStr;
        }
        else
        {
            serialized += ERROR_SEPARATOR + errorStr;
        }

        addMetadata( ref, MODEL_ERRORS, serialized );
        fireErrorEvent( ref, error );
    }

    private String toString( final Throwable e )
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter( sw );

        e.printStackTrace( pw );

        return sw.toString();
    }

    @Override
    public Map<ProjectVersionRef, Set<String>> getAllProjectErrors()
        throws CartoDataException
    {
        final Set<ProjectVersionRef> projects = graphs.getProjectsWithMetadata( MODEL_ERRORS );
        final Map<ProjectVersionRef, Set<String>> errors = new HashMap<ProjectVersionRef, Set<String>>( projects.size() );

        for ( final ProjectVersionRef project : projects )
        {
            errors.put( project, getErrors( project ) );
        }

        return errors;
    }

    @Override
    public Map<ProjectVersionRef, Set<String>> getProjectErrorsInGraph( final ProjectVersionRef ref )
        throws CartoDataException
    {
        logger.info( "Looking up graph for: %s", ref );
        final EProjectGraph graph = graphs.getGraph( workspaceHolder.getCurrentWorkspace(), ref );

        logger.info( "Querying graph: %s for projects with errors.", graph );

        if ( graph == null )
        {
            logger.info( "Graph is null. Returning null results." );
            return null;
        }

        final Set<ProjectVersionRef> projects = graph.getProjectsWithMetadata( MODEL_ERRORS );

        if ( projects == null )
        {
            return null;
        }

        final Map<ProjectVersionRef, Set<String>> errors = new HashMap<ProjectVersionRef, Set<String>>( projects.size() );

        for ( final ProjectVersionRef project : projects )
        {
            errors.put( project, getErrors( project ) );
        }

        return errors;
    }

    @Override
    public void reindex( final ProjectVersionRef ref )
        throws CartoDataException
    {
        final EProjectGraph graph = graphs.getGraph( workspaceHolder.getCurrentWorkspace(), ref );

        if ( graph == null )
        {
            throw new CartoDataException( "Graph not found: %s", ref );
        }

        try
        {
            graph.reindex();
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to reindex graph: %s. Reason: %s", e, ref, e.getMessage() );
        }
    }

    @Override
    public void reindexAll()
        throws CartoDataException
    {
        try
        {
            graphs.reindex( getCurrentWorkspace() );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to reindex global graph. Reason: %s", e, e.getMessage() );
        }
    }

    @Override
    public EProjectWeb getProjectWeb( final ProjectVersionRef... refs )
        throws CartoDataException
    {
        return graphs.getWeb( workspaceHolder.getCurrentWorkspace(), refs );
    }

    @Override
    public EProjectWeb getProjectWeb( final ProjectRelationshipFilter filter, final ProjectVersionRef... refs )
        throws CartoDataException
    {
        return graphs.getWeb( workspaceHolder.getCurrentWorkspace(), filter, refs );
    }

    @Override
    public Set<ProjectVersionRef> getMatchingGAVs( final ProjectRef projectRef )
        throws CartoDataException
    {
        return graphs.getProjectsMatching( projectRef, workspaceHolder.getCurrentWorkspace() );
    }

    @Override
    public Set<ProjectVersionRef> pathFilter( final ProjectRelationshipFilter filter, final Set<ProjectVersionRef> leaves,
                                              final ProjectVersionRef... refs )
        throws CartoDataException
    {
        if ( filter == null )
        {
            return leaves;
        }

        final EProjectNet web = getProjectWeb( refs );

        logger.info( "BEFORE filtering: %d leaf projects:\n  %s", leaves.size(), join( leaves, "\n  " ) );

        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
        logger.info( "Looking for paths to missing projects: %s in network: %s filtered by: %s", join( leaves, ", " ), web, filter );

        final Set<List<ProjectRelationship<?>>> paths = web.getPathsTo( leaves.toArray( new ProjectVersionRef[] {} ) );
        if ( paths != null )
        {
            nextPath: for ( final List<ProjectRelationship<?>> path : paths )
            {
                if ( path.isEmpty() )
                {
                    continue;
                }

                final ProjectVersionRef target = path.get( path.size() - 1 )
                                                     .getTarget()
                                                     .asProjectVersionRef();
                if ( result.contains( target ) )
                {
                    continue;
                }

                ProjectRelationshipFilter f = filter;
                for ( final ProjectRelationship<?> rel : path )
                {
                    if ( !f.accept( rel ) )
                    {
                        logger.info( "Path: %s rejected, trying any others that remain.", path );
                        continue nextPath;
                    }

                    f = f.getChildFilter( rel );
                }

                result.add( target );
            }
        }

        logger.info( "AFTER filtering: %d leaf projects:\n  %s", result.size(), join( result, "\n  " ) );

        return result;
    }

    public void closeCurrentWorkspace()
        throws CartoDataException
    {
        final GraphWorkspace ws = workspaceHolder.getCurrentWorkspace();
        if ( ws != null )
        {
            try
            {
                ws.close();
            }
            catch ( final IOException e )
            {
                throw new CartoDataException( "Failed to close workspace: %s. Reason: %s", e, ws.getId(), e.getMessage() );
            }
        }
    }

    @Override
    public GraphWorkspace setCurrentWorkspace( final String id )
        throws CartoDataException
    {
        try
        {
            final GraphWorkspace ws = graphs.getWorkspace( id );

            if ( ws != null )
            {
                ws.addListener( this );
                workspaceHolder.setCurrentWorkspace( ws, true );
            }

            return ws;
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to retrieve workspace: %s. Error: %s", e, id, e.getMessage() );
        }
    }

    @Override
    public GraphWorkspace createWorkspace( final URI sourceUri )
        throws CartoDataException
    {
        return createWorkspace( new GraphWorkspaceConfiguration().withSource( sourceUri ) );
    }

    private void checkForCurrentWorkspace()
        throws CartoDataException
    {
        if ( workspaceHolder.getCurrentWorkspace() != null )
        {
            throw new CartoDataException( "You already have an active workspace! Close that one before creating a new one." );
        }
    }

    @Override
    public GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws CartoDataException
    {
        checkForCurrentWorkspace();

        GraphWorkspace workspace;
        try
        {
            workspace = graphs.createWorkspace( config )
                              .addListener( this );

            workspaceHolder.setCurrentWorkspace( workspace, true );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to initialize session with config: %s. Reason: %s", e, config, e.getMessage() );
        }

        return workspace;
    }

    @Override
    public void closed( final GraphWorkspace ws )
    {
        if ( workspaceHolder.getCurrentWorkspace() == ws )
        {
            workspaceHolder.clearCurrentWorkspace();
        }
    }

    @Override
    public void detached( final GraphWorkspace ws )
    {
        clearCurrentWorkspace();
    }

    @Override
    public boolean deleteWorkspace( final String id )
        throws CartoDataException
    {
        try
        {
            return graphs.deleteWorkspace( id );
        }
        catch ( final IOException e )
        {
            throw new CartoDataException( "Failed to delete workspace: %s. Reason: %s", e, id, e.getMessage() );
        }
    }

    @Override
    public Set<GraphWorkspace> getAllWorkspaces()
    {
        return graphs.getAllWorkspaces();
    }

    @Override
    public GraphWorkspace getCurrentWorkspace()
    {
        return workspaceHolder.getCurrentWorkspace();
    }

    @Override
    public GraphWorkspace createTemporaryWorkspace( final GraphWorkspaceConfiguration config )
        throws CartoDataException
    {
        checkForCurrentWorkspace();

        GraphWorkspace workspace;
        try
        {
            workspace = graphs.createTemporaryWorkspace( config )
                              .addListener( this );

            workspaceHolder.setCurrentWorkspace( workspace, true );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to initialize session with config: %s. Reason: %s", e, config, e.getMessage() );
        }

        return workspace;
    }

    @Override
    public void clearCurrentWorkspace()
    {
        workspaceHolder.clearCurrentWorkspace();
    }

    @Override
    public EGraphManager getGraphManager()
    {
        return graphs;
    }

    @Override
    public GraphWorkspace getWorkspace( final String id )
        throws CartoDataException
    {
        try
        {
            return graphs.getWorkspace( id );
        }
        catch ( final GraphDriverException e )
        {
            throw new CartoDataException( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
        }
    }

}
