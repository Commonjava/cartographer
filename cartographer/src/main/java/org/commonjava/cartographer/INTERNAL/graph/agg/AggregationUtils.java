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
package org.commonjava.cartographer.INTERNAL.graph.agg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.cartographer.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.graph.agg.ProjectRefCollection;
import org.commonjava.maven.galley.model.ConcreteResource;

public final class AggregationUtils
{

    private AggregationUtils()
    {
    }

    public static Map<ProjectRef, ProjectRefCollection> collectProjectReferences( final RelationshipGraph graph )
    {
        final Collection<ProjectRelationship<?, ?>> rels = graph.getAllRelationships();
        return collectProjectReferences( rels );
    }

    public static Map<ProjectRef, ProjectRefCollection> collectProjectReferences( final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> refMap )
    {
        final Map<ProjectRef, ProjectRefCollection> projects = new HashMap<>();

        for ( final Map.Entry<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> projectEntry : refMap.entrySet() )
        {
            final ProjectVersionRef pvr = projectEntry.getKey();
            final ProjectRef r = pvr.asProjectRef();

            ProjectRefCollection prc = projects.get( r );
            if ( prc == null )
            {
                prc = new ProjectRefCollection();
                projects.put( r, prc );
            }

            prc.addVersionRef( pvr );
            prc.addArtifactRef( pvr.asPomArtifact() );

            for ( final ArtifactRef ar : projectEntry.getValue()
                                                     .keySet() )
            {
                prc.addArtifactRef( ar );
            }
        }

        return projects;
    }

    public static Map<ProjectRef, ProjectRefCollection> collectProjectReferences( final Collection<ProjectRelationship<?, ?>> rels )
    {
        final Map<ProjectRef, ProjectRefCollection> projects = new HashMap<ProjectRef, ProjectRefCollection>();

        for ( final ProjectRelationship<?, ?> rel : rels )
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

            final ArtifactRef tar = rel.getTargetArtifact();

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

    public static Map<ProjectVersionRef, ProjectRefCollection> collectProjectVersionReferences( final RelationshipGraph graph )
    {
        final Collection<ProjectRelationship<?, ?>> rels = graph.getAllRelationships();
        final Map<ProjectVersionRef, ProjectRefCollection> result = collectProjectVersionReferences( rels );
        for ( final ProjectVersionRef root : graph.getParams()
                                                  .getRoots() )
        {
            ProjectRefCollection collection = result.get( root );
            if ( collection == null )
            {
                collection = new ProjectRefCollection();
                result.put( root, collection );
            }

            collection.addArtifactRef( root.asPomArtifact() );
        }

        return result;
    }

    public static Map<ProjectVersionRef, ProjectRefCollection> collectProjectVersionReferences( final Collection<ProjectRelationship<?, ?>> rels )
    {
        final Map<ProjectVersionRef, ProjectRefCollection> projects =
            new HashMap<ProjectVersionRef, ProjectRefCollection>();

        for ( final ProjectRelationship<?, ?> rel : rels )
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

            final ArtifactRef tar = rel.getTargetArtifact();

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

    public static Set<ArtifactRef> collectArtifactReferences( final RelationshipGraph graph,
                                                              final boolean includePomArtifacts )
    {
        final Collection<ProjectRelationship<?, ?>> rels = graph.getAllRelationships();
        return collectArtifactReferences( rels, includePomArtifacts );
    }

    public static Set<ArtifactRef> collectArtifactReferences( final Collection<ProjectRelationship<?, ?>> rels,
                                                              final boolean includePomArtifacts )
    {
        final Set<ArtifactRef> artifacts = new HashSet<ArtifactRef>();

        for ( final ProjectRelationship<?, ?> rel : rels )
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

            final ArtifactRef tar = rel.getTargetArtifact();

            artifacts.add( tar );
            if ( includePomArtifacts )
            {
                artifacts.add( tar.asPomArtifact() );
            }

        }

        return artifacts;
    }

}
