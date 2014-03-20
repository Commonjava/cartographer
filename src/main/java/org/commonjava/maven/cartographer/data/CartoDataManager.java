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
package org.commonjava.maven.cartographer.data;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface CartoDataManager
{

    String MODEL_ERRORS = "modelErrors";

    String ERROR_SEPARATOR = Pattern.quote( "_::--::_" );

    Set<ProjectRelationship<?>> storeRelationships( ProjectRelationship<?>... relationships )
        throws CartoDataException;

    Set<ProjectRelationship<?>> storeRelationships( Collection<ProjectRelationship<?>> relationships )
        throws CartoDataException;

    EProjectGraph getProjectGraph( ProjectRelationshipFilter filter, ProjectVersionRef discovered )
        throws CartoDataException;

    EProjectGraph getProjectGraph( ProjectVersionRef ref )
        throws CartoDataException;

    List<ProjectVersionRef> getAncestry( ProjectVersionRef source )
        throws CartoDataException;

    ProjectVersionRef getParent( ProjectVersionRef source )
        throws CartoDataException;

    Set<ProjectVersionRef> getKnownChildren( ProjectVersionRef parent )
        throws CartoDataException;

    /**
     * @deprecated Use {@link #getAllDirectRelationshipsWithExactSource(ProjectVersionRef,ProjectRelationshipFilter,GraphMutator)} instead
     */
    @Deprecated
    Set<ProjectRelationship<?>> getAllDirectRelationshipsWithExactSource( ProjectVersionRef source, ProjectRelationshipFilter filter )
        throws CartoDataException;

    Set<ProjectRelationship<?>> getAllDirectRelationshipsWithExactSource( ProjectVersionRef source, ProjectRelationshipFilter filter,
                                                                          GraphMutator mutator )
        throws CartoDataException;

    /**
     * @deprecated Use {@link #getAllDirectRelationshipsWithExactTarget(ProjectVersionRef,ProjectRelationshipFilter,GraphMutator)} instead
     */
    @Deprecated
    Set<ProjectRelationship<?>> getAllDirectRelationshipsWithExactTarget( ProjectVersionRef target, ProjectRelationshipFilter filter )
        throws CartoDataException;

    Set<ProjectRelationship<?>> getAllDirectRelationshipsWithExactTarget( ProjectVersionRef target, ProjectRelationshipFilter filter,
                                                                          GraphMutator mutator )
        throws CartoDataException;

    Set<ProjectRelationship<?>> getAllDirectRelationshipsWithGASource( ProjectRef source, ProjectRelationshipFilter filter )
        throws CartoDataException;

    Set<ProjectRelationship<?>> getAllDirectRelationshipsWithGATarget( ProjectRef target, ProjectRelationshipFilter filter )
        throws CartoDataException;

    boolean contains( ProjectVersionRef ref );

    Set<ProjectVersionRef> getAllStoredProjectRefs()
        throws CartoDataException;

    Map<String, String> getMetadata( ProjectVersionRef ref )
        throws CartoDataException;

    void addMetadata( ProjectVersionRef ref, String name, String value )
        throws CartoDataException;

    void addMetadata( ProjectVersionRef ref, Map<String, String> metadata )
        throws CartoDataException;

    Set<ProjectVersionRef> getIncompleteSubgraphsFor( ProjectVersionRef ref )
        throws CartoDataException;

    /**
     * @deprecated Use {@link #getIncompleteSubgraphsFor(ProjectRelationshipFilter,GraphMutator,ProjectVersionRef)} instead
     */
    @Deprecated
    Set<ProjectVersionRef> getIncompleteSubgraphsFor( ProjectRelationshipFilter filter, ProjectVersionRef ref )
        throws CartoDataException;

    Set<ProjectVersionRef> getIncompleteSubgraphsFor( ProjectRelationshipFilter filter, GraphMutator mutator, ProjectVersionRef ref )
        throws CartoDataException;

    Set<ProjectVersionRef> getVariableSubgraphsFor( ProjectVersionRef ref )
        throws CartoDataException;

    /**
     * @deprecated Use {@link #getVariableSubgraphsFor(ProjectRelationshipFilter,GraphMutator,ProjectVersionRef)} instead
     */
    @Deprecated
    Set<ProjectVersionRef> getVariableSubgraphsFor( ProjectRelationshipFilter filter, ProjectVersionRef ref )
        throws CartoDataException;

    Set<ProjectVersionRef> getVariableSubgraphsFor( ProjectRelationshipFilter filter, GraphMutator mutator, ProjectVersionRef ref )
        throws CartoDataException;

    /**
     * @deprecated Use {@link #getAllIncompleteSubgraphs(ProjectRelationshipFilter,GraphMutator)} instead
     */
    @Deprecated
    Set<ProjectVersionRef> getAllIncompleteSubgraphs( ProjectRelationshipFilter filter )
        throws CartoDataException;

    Set<ProjectVersionRef> getAllIncompleteSubgraphs( ProjectRelationshipFilter filter, GraphMutator mutator )
        throws CartoDataException;

    Set<ProjectVersionRef> getAllIncompleteSubgraphs()
        throws CartoDataException;

    /**
     * @deprecated Use {@link #getAllVariableSubgraphs(ProjectRelationshipFilter,GraphMutator)} instead
     */
    @Deprecated
    Set<ProjectVersionRef> getAllVariableSubgraphs( ProjectRelationshipFilter filter )
        throws CartoDataException;

    Set<ProjectVersionRef> getAllVariableSubgraphs( ProjectRelationshipFilter filter, GraphMutator mutator )
        throws CartoDataException;

    Set<ProjectVersionRef> getAllVariableSubgraphs()
        throws CartoDataException;

    void addError( final EProjectKey key, final Throwable error )
        throws CartoDataException;

    void clearErrors( final ProjectVersionRef ref )
        throws CartoDataException;

    Set<String> getErrors( final ProjectVersionRef ref )
        throws CartoDataException;

    boolean hasErrors( final ProjectVersionRef ref )
        throws CartoDataException;

    Map<ProjectVersionRef, Set<String>> getAllProjectErrors()
        throws CartoDataException;

    Map<ProjectVersionRef, Set<String>> getProjectErrorsInGraph( ProjectVersionRef ref )
        throws CartoDataException;

    void reindex( ProjectVersionRef ref )
        throws CartoDataException;

    void reindexAll()
        throws CartoDataException;

    EProjectWeb getProjectWeb( ProjectRelationshipFilter filter, ProjectVersionRef... refs )
        throws CartoDataException;

    EProjectWeb getProjectWeb( ProjectVersionRef... refs )
        throws CartoDataException;

    Set<ProjectVersionRef> pathFilter( ProjectRelationshipFilter filter, Set<ProjectVersionRef> leaves, ProjectVersionRef... roots )
        throws CartoDataException;

    GraphWorkspace createWorkspace( String id, URI sourceUri )
        throws CartoDataException;

    GraphWorkspace createWorkspace( URI sourceUri )
        throws CartoDataException;

    GraphWorkspace createWorkspace( String id, GraphWorkspaceConfiguration config )
        throws CartoDataException;

    GraphWorkspace createWorkspace( GraphWorkspaceConfiguration config )
        throws CartoDataException;

    GraphWorkspace createTemporaryWorkspace( GraphWorkspaceConfiguration config )
        throws CartoDataException;

    GraphWorkspace setCurrentWorkspace( String sessionId )
        throws CartoDataException;

    Set<ProjectVersionRef> getMatchingGAVs( ProjectRef projectRef )
        throws CartoDataException;

    boolean deleteWorkspace( String id )
        throws CartoDataException;

    Set<GraphWorkspace> getAllWorkspaces();

    GraphWorkspace getCurrentWorkspace();

    void clearCurrentWorkspace()
        throws CartoDataException;

    @Deprecated
    EGraphManager getGraphManager();

    EGraphManager graphs();

    GraphWorkspace getWorkspace( String id )
        throws CartoDataException;

    Map<Map<String, String>, Set<ProjectVersionRef>> collateProjectsByMetadata( Set<ProjectVersionRef> refs, Set<String> keys )
        throws CartoDataException;

}
