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
package org.commonjava.maven.cartographer.ops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.util.ProjectVersionRefComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GraphOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public BuildOrder getBuildOrder( final RelationshipGraph graph )
        throws CartoDataException
    {
        if ( graph != null )
        {
            final BuildOrderTraversal traversal = new BuildOrderTraversal();

            logger.info( "Performing build-order traversal for graph: {}", graph );
            try
            {
                graph.traverse( traversal );
            }
            catch ( final RelationshipGraphException e )
            {
                throw new CartoDataException( "Failed to construct build order for: {}. Reason: {}", e, graph,
                                              e.getMessage() );
            }

            return traversal.getBuildOrder();
        }

        return null;
    }

    public List<ProjectVersionRef> listProjects( final String groupIdPattern, final String artifactIdPattern,
                                                 final RelationshipGraph graph )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> all = graph.getAllProjects();
        final List<ProjectVersionRef> matching = new ArrayList<ProjectVersionRef>();
        if ( all != null )
        {
            if ( groupIdPattern != null || artifactIdPattern != null )
            {
                final String gip = groupIdPattern == null ? ".*" : groupIdPattern.replaceAll( "\\*", ".*" );
                final String aip = artifactIdPattern == null ? ".*" : artifactIdPattern.replaceAll( "\\*", ".*" );

                logger.info( "Filtering {} projects using groupId pattern: '{}' and artifactId pattern: '{}'", all.size(), gip, aip );

                for ( final ProjectVersionRef ref : all )
                {
                    if ( ref.getGroupId()
                            .matches( gip ) && ref.getArtifactId()
                                                  .matches( aip ) )
                    {
                        matching.add( ref );
                    }
                }
            }
            else
            {
                logger.info( "Returning all {} projects", all.size() );
                matching.addAll( all );
            }

        }

        if ( !matching.isEmpty() )
        {
            Collections.sort( matching, new ProjectVersionRefComparator() );

        }

        return matching;
    }

}
