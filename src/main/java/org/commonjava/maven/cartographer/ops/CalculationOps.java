package org.commonjava.maven.cartographer.ops;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphDifference;

@ApplicationScoped
public class CalculationOps
{

    @Inject
    private CartoDataManager data;

    protected CalculationOps()
    {
    }

    public CalculationOps( final CartoDataManager data )
    {
        this.data = data;
    }

    public GraphDifference difference( final Set<ProjectVersionRef> firstProjects,
                                       final ProjectRelationshipFilter firstFilter,
                                       final Set<ProjectVersionRef> secondProjects,
                                       final ProjectRelationshipFilter secondFilter )
        throws CartoDataException
    {
        final EProjectWeb first =
            data.getProjectWeb( firstFilter, firstProjects.toArray( new ProjectVersionRef[firstProjects.size()] ) );

        final EProjectWeb second =
            data.getProjectWeb( secondFilter, secondProjects.toArray( new ProjectVersionRef[secondProjects.size()] ) );

        final Set<ProjectRelationship<?>> firstAll = first.getAllRelationships();
        final Set<ProjectRelationship<?>> secondAll = second.getAllRelationships();

        final Set<ProjectRelationship<?>> added = new HashSet<ProjectRelationship<?>>( secondAll );
        added.removeAll( firstAll );

        final Set<ProjectRelationship<?>> removed = new HashSet<ProjectRelationship<?>>( firstAll );
        removed.removeAll( secondAll );

        return new GraphDifference( firstProjects, firstFilter, secondProjects, secondFilter, added, removed );
    }

    public GraphCalculation subtract( final Set<ProjectVersionRef> firstProjects,
                                      final ProjectRelationshipFilter firstFilter,
                                      final Set<ProjectVersionRef> secondProjects,
                                      final ProjectRelationshipFilter secondFilter )
        throws CartoDataException
    {
        final EProjectWeb first =
            data.getProjectWeb( firstFilter, firstProjects.toArray( new ProjectVersionRef[firstProjects.size()] ) );

        final EProjectWeb second =
            data.getProjectWeb( secondFilter, secondProjects.toArray( new ProjectVersionRef[secondProjects.size()] ) );

        final Set<ProjectRelationship<?>> firstAll = first.getAllRelationships();
        final Set<ProjectRelationship<?>> secondAll = second.getAllRelationships();

        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>( firstAll );
        result.removeAll( secondAll );

        return new GraphCalculation( GraphCalculation.Type.SUBTRACTION, firstProjects, firstFilter, secondProjects,
                                     secondFilter, result );
    }

    public GraphCalculation add( final Set<ProjectVersionRef> firstProjects,
                                 final ProjectRelationshipFilter firstFilter,
                                 final Set<ProjectVersionRef> secondProjects,
                                 final ProjectRelationshipFilter secondFilter )
        throws CartoDataException
    {
        final EProjectWeb first =
            data.getProjectWeb( firstFilter, firstProjects.toArray( new ProjectVersionRef[firstProjects.size()] ) );

        final EProjectWeb second =
            data.getProjectWeb( secondFilter, secondProjects.toArray( new ProjectVersionRef[secondProjects.size()] ) );

        final Set<ProjectRelationship<?>> firstAll = first.getAllRelationships();
        final Set<ProjectRelationship<?>> secondAll = second.getAllRelationships();

        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>( firstAll );
        result.addAll( secondAll );

        return new GraphCalculation( GraphCalculation.Type.ADDITION, firstProjects, firstFilter, secondProjects,
                                     secondFilter, result );
    }

    public GraphCalculation intersection( final Set<ProjectVersionRef> firstProjects,
                                          final ProjectRelationshipFilter firstFilter,
                                          final Set<ProjectVersionRef> secondProjects,
                                          final ProjectRelationshipFilter secondFilter )
        throws CartoDataException
    {
        final EProjectWeb first =
            data.getProjectWeb( firstFilter, firstProjects.toArray( new ProjectVersionRef[firstProjects.size()] ) );

        final EProjectWeb second =
            data.getProjectWeb( secondFilter, secondProjects.toArray( new ProjectVersionRef[secondProjects.size()] ) );

        final Set<ProjectRelationship<?>> firstAll = first.getAllRelationships();
        final Set<ProjectRelationship<?>> secondAll = second.getAllRelationships();

        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>( firstAll );
        result.retainAll( secondAll );

        return new GraphCalculation( GraphCalculation.Type.INTERSECTION, firstProjects, firstFilter, secondProjects,
                                     secondFilter, result );
    }

}
