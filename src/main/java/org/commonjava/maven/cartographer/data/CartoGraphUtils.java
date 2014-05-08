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

    public static Map<ProjectVersionRef, Throwable> getAllProjectErrors( final RelationshipGraph graph )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> projects = graph.getAllProjects();
        final Map<ProjectVersionRef, Throwable> errors = new HashMap<ProjectVersionRef, Throwable>();

        for ( final ProjectVersionRef project : projects )
        {
            final Throwable error = graph.getProjectError( project );
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
