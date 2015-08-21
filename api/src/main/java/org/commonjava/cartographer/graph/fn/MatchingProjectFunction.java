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
package org.commonjava.cartographer.graph.fn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ProjectVersionRefComparator;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.request.ProjectGraphRequest;

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
