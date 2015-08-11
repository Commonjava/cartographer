package org.commonjava.maven.cartographer.ops.fn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ProjectVersionRefComparator;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.request.ProjectGraphRequest;

public class MatchingProjectFunction<T>
    implements GraphFunction
{

    private final ProjectGraphRequest recipe;

    private final ProjectProjector<T> extractor;

    private final ProjectCollector<T> consumer;

    private ProjectSelector supplier;

    public MatchingProjectFunction( final ProjectGraphRequest recipe, final ProjectProjector<T> extractor,
                                     final ProjectCollector<T> consumer )
    {
        this.recipe = recipe;
        this.extractor = extractor;
        this.consumer = consumer;
        this.supplier = ( graph ) -> {
            return graph.getAllProjects();
        };
    }

    public MatchingProjectFunction( final ProjectGraphRequest recipe, final ProjectProjector<T> extractor,
                                     final ProjectCollector<T> consumer, final ProjectSelector supplier )
    {
        this.recipe = recipe;
        this.extractor = extractor;
        this.consumer = consumer;
        this.supplier = supplier;
    }

    @Override
    public void extract( final RelationshipGraph graph )
                    throws CartoRequestException, CartoDataException
    {
        final ProjectVersionRef ref = recipe.getProject();
        if ( ref != null )
        {
            consumer.accept( ref, extractor.extract( ref, graph ) );
        }

        final String matcher = recipe.getProjectGavPattern();
        final List<ProjectVersionRef> projects = new ArrayList<>( supplier.getProjects( graph ) );
        Collections.sort( projects, new ProjectVersionRefComparator() );

        for ( final ProjectVersionRef project : projects )
        {
            if ( StringUtils.isEmpty( matcher ) || project.toString()
                                                          .matches( matcher ) )
            {
                consumer.accept( project, extractor.extract( project, graph ) );
            }
        }
    }

}
