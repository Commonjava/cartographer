package org.commonjava.maven.cartographer.agg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class AggregationUtils
{

    private AggregationUtils()
    {
    }

    public static Map<ProjectRef, ProjectRefCollection> collectProjectReferences( final EProjectWeb web )
    {
        final Set<ProjectRelationship<?>> rels = web.getAllRelationships();
        return collectProjectReferences( rels );
    }

    public static Map<ProjectRef, ProjectRefCollection> collectProjectReferences( final Set<ProjectRelationship<?>> rels )
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

    public static Map<ProjectVersionRef, ProjectRefCollection> collectProjectVersionReferences( final EProjectWeb web )
    {
        final Set<ProjectRelationship<?>> rels = web.getAllRelationships();
        return collectProjectVersionReferences( rels );
    }

    public static Map<ProjectVersionRef, ProjectRefCollection> collectProjectVersionReferences( final Set<ProjectRelationship<?>> rels )
    {
        final Map<ProjectVersionRef, ProjectRefCollection> projects = new HashMap<>();

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

    public static Set<ArtifactRef> collectArtifactReferences( final EProjectWeb web, final boolean includePomArtifacts )
    {
        final Set<ProjectRelationship<?>> rels = web.getAllRelationships();
        return collectArtifactReferences( rels, includePomArtifacts );
    }

    public static Set<ArtifactRef> collectArtifactReferences( final Set<ProjectRelationship<?>> rels, final boolean includePomArtifacts )
    {
        final Set<ArtifactRef> artifacts = new HashSet<>();

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
