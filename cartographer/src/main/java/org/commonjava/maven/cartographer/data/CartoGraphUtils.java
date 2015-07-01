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
package org.commonjava.maven.cartographer.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.slf4j.LoggerFactory;

public final class CartoGraphUtils
{

    private CartoGraphUtils()
    {
    }

    public static List<ProjectVersionRef> getAncestry( final ProjectVersionRef source, final RelationshipGraph graph )
        throws RelationshipGraphException
    {
        final AncestryTraversal ancestryTraversal = new AncestryTraversal();

        graph.traverse( source, ancestryTraversal, TraversalType.depth_first );

        return ancestryTraversal.getAncestry();
    }

    public static ProjectVersionRef getParent( final ProjectVersionRef source, final RelationshipGraph graph )
    {
        final Set<ProjectRelationship<?>> matches =
            graph.findDirectRelationshipsFrom( source, false, RelationshipType.PARENT );

        if ( matches != null && !matches.isEmpty() )
        {
            final ParentRelationship parent = (ParentRelationship) matches.iterator()
                                                                          .next();
            return parent.getTarget();
        }

        return null;
    }

    public static Set<ProjectVersionRef> getKnownChildren( final ProjectVersionRef parent, final RelationshipGraph graph )
        throws CartoDataException
    {
        final Set<ProjectRelationship<?>> matches =
            graph.findDirectRelationshipsTo( parent, false, RelationshipType.PARENT );

        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : matches )
        {
            refs.add( rel.getDeclaring() );
        }

        return refs;
    }

    public static Set<ProjectRelationship<?>> getAllDirectRelationshipsWithGASource( final ProjectRef source,
                                                                                     final RelationshipGraph graph,
                                                                                     final boolean managed,
                                                                                     final RelationshipType... types )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> refs = graph.getProjectsMatching( source );
        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Set<ProjectRelationship<?>> rels = graph.findDirectRelationshipsFrom( ref, managed, types );
            if ( rels != null )
            {
                result.addAll( rels );
            }
        }

        return result;
    }

    public static Set<ProjectRelationship<?>> getAllDirectRelationshipsWithGATarget( final ProjectRef target,
                                                                                     final RelationshipGraph graph,
                                                                                     final boolean managed,
                                                                                     final RelationshipType... types )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> refs = graph.getProjectsMatching( target );
        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Set<ProjectRelationship<?>> rels = graph.findDirectRelationshipsTo( ref, managed, types );
            if ( rels != null )
            {
                result.addAll( rels );
            }
        }

        return result;
    }

    public static Map<ProjectVersionRef, String> getAllProjectErrors( final RelationshipGraph graph )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> projects = graph.getAllProjects();
        final Map<ProjectVersionRef, String> errors = new HashMap<ProjectVersionRef, String>();

        for ( final ProjectVersionRef project : projects )
        {
            final String error = graph.getProjectError( project );
            if ( error != null )
            {
                errors.put( project, error );
            }
        }

        return errors;
    }

    public static void closeGraphQuietly( final RelationshipGraph graph )
    {
        if ( graph != null )
        {
            try
            {
                graph.close();
            }
            catch ( final RelationshipGraphException e )
            {

                LoggerFactory.getLogger( CartoGraphUtils.class )
                             .error( String.format( "Failed to close workspace: %s. Reason: %s", graph, e.getMessage() ),
                                     e );
            }
        }
    }

    public static BuildOrder getBuildOrder( final ProjectVersionRef ref, final RelationshipGraph graph )
        throws CartoDataException
    {
        final BuildOrderTraversal traversal = new BuildOrderTraversal();
        try
        {
            graph.traverse( ref, traversal, TraversalType.breadth_first );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException( "Traversal to capture build order failed for: {} in graph: {}. Reason: {}",
                                          e, ref, graph, e.getMessage() );
        }

        return traversal.getBuildOrder();
    }
}
