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
package org.commonjava.maven.cartographer.agg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class AggregationUtils
{

    private AggregationUtils()
    {
    }

    public static Map<ProjectRef, ProjectRefCollection> collectProjectReferences( final EProjectNet web )
    {
        final Collection<ProjectRelationship<?>> rels = web.getAllRelationships();
        return collectProjectReferences( rels );
    }

    public static Map<ProjectRef, ProjectRefCollection> collectProjectReferences( final Collection<ProjectRelationship<?>> rels )
    {
        final Map<ProjectRef, ProjectRefCollection> projects = new HashMap<ProjectRef, ProjectRefCollection>();

        for ( final ProjectRelationship<?> rel : rels )
        {
            //            if ( !( rel instanceof DependencyRelationship ) )
            //            {
            //                continue;
            //            }

            final ProjectVersionRef orig = rel.getDeclaring();
            final ProjectVersionRef pvr = orig.asProjectVersionRef();
            final ProjectRef r = rel.getDeclaring()
                                    .asProjectRef();
            ProjectRefCollection prc = projects.get( r );
            if ( prc == null )
            {
                prc = new ProjectRefCollection();
                projects.put( r, prc );
            }

            prc.addVersionRef( pvr );
            prc.addArtifactRef( pvr.asPomArtifact() );

            final ArtifactRef tar = rel.getTargetArtifact()
                                       .setOptional( false );

            final ProjectRef tr = tar.asProjectRef();
            ProjectRefCollection tprc = projects.get( tr );
            if ( tprc == null )
            {
                tprc = new ProjectRefCollection();
                projects.put( tr, tprc );
            }

            tprc.addArtifactRef( tar );
        }

        return projects;
    }

    public static Map<ProjectVersionRef, ProjectRefCollection> collectProjectVersionReferences( final EProjectNet web )
    {
        final Collection<ProjectRelationship<?>> rels = web.getAllRelationships();
        return collectProjectVersionReferences( rels );
    }

    public static Map<ProjectVersionRef, ProjectRefCollection> collectProjectVersionReferences( final Collection<ProjectRelationship<?>> rels )
    {
        final Map<ProjectVersionRef, ProjectRefCollection> projects = new HashMap<ProjectVersionRef, ProjectRefCollection>();

        for ( final ProjectRelationship<?> rel : rels )
        {
            //            if ( !( rel instanceof DependencyRelationship ) )
            //            {
            //                continue;
            //            }

            final ProjectVersionRef pvr = rel.getDeclaring()
                                             .asProjectVersionRef();

            ProjectRefCollection prc = projects.get( pvr );
            if ( prc == null )
            {
                prc = new ProjectRefCollection();
                projects.put( pvr, prc );
            }

            prc.addVersionRef( pvr );
            prc.addArtifactRef( pvr.asPomArtifact() );

            final ArtifactRef tar = rel.getTargetArtifact()
                                       .setOptional( false );

            final ProjectVersionRef tr = tar.asProjectVersionRef();
            ProjectRefCollection tprc = projects.get( tr );
            if ( tprc == null )
            {
                tprc = new ProjectRefCollection();
                projects.put( tr, tprc );
            }

            tprc.addArtifactRef( tar );
        }

        return projects;
    }

    public static Set<ArtifactRef> collectArtifactReferences( final EProjectNet web, final boolean includePomArtifacts )
    {
        final Collection<ProjectRelationship<?>> rels = web.getAllRelationships();
        return collectArtifactReferences( rels, includePomArtifacts );
    }

    public static Set<ArtifactRef> collectArtifactReferences( final Collection<ProjectRelationship<?>> rels, final boolean includePomArtifacts )
    {
        final Set<ArtifactRef> artifacts = new HashSet<ArtifactRef>();

        for ( final ProjectRelationship<?> rel : rels )
        {
            //            if ( !( rel instanceof DependencyRelationship ) )
            //            {
            //                continue;
            //            }

            if ( includePomArtifacts )
            {
                final ProjectVersionRef pvr = rel.getDeclaring()
                                                 .asProjectVersionRef();

                artifacts.add( pvr.asPomArtifact() );
            }

            final ArtifactRef tar = rel.getTargetArtifact()
                                       .setOptional( false );

            artifacts.add( tar );
            if ( includePomArtifacts )
            {
                artifacts.add( tar.asPomArtifact() );
            }

        }

        return artifacts;
    }

}
