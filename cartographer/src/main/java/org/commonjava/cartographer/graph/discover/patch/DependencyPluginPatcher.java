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
package org.commonjava.cartographer.graph.discover.patch;

import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.*;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.cartographer.INTERNAL.graph.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;

public class DependencyPluginPatcher
    implements DepgraphPatcher
{

    private static final String[] PATHS =
        {
            "/project/build/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/executions/execution/configuration/artifactItems/artifactItem",
            "/project/build/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/configuration/artifactItems/artifactItem",
            "/project/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/executions/execution/configuration/artifactItems/artifactItem",
            "/project/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/configuration/artifactItems/artifactItem",
            "/project/profiles/profile/build/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/executions/execution/configuration/artifactItems/artifactItem",
            "/project/profiles/profile/build/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/configuration/artifactItems/artifactItem",
            "/project/profiles/profile/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/executions/execution/configuration/artifactItems/artifactItem",
            "/project/profiles/profile/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-dependency-plugin\"]/configuration/artifactItems/artifactItem" };

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void patch( final DiscoveryResult result, final List<? extends Location> locations,
                       final Map<String, Object> context )
    {
        final ProjectVersionRef ref = result.getSelectedRef();
        try
        {
            final MavenPomView pomView = (MavenPomView) context.get( POM_VIEW_CTX_KEY );

            // get all artifactItems with a version element. Only these need to be verified.
            final String depArtifactItemsPath = join( PATHS, "|" );

            logger.debug( "Looking for dependency-plugin usages matching: '{}'", depArtifactItemsPath );
            // TODO: Switch to a DependencyView here, with path to dependencyManagement...
            final List<DependencyView> depArtifactItems = pomView.getAllDependenciesMatching( depArtifactItemsPath );
            if ( depArtifactItems == null || depArtifactItems.isEmpty() )
            {
                return;
            }

            final Set<ProjectRelationship<?, ?>> accepted =
                new HashSet<ProjectRelationship<?, ?>>( result.getAcceptedRelationships() );

            final Map<VersionlessArtifactRef, DependencyRelationship> concreteDeps =
                new HashMap<VersionlessArtifactRef, DependencyRelationship>();
            for ( final ProjectRelationship<?, ?> rel : accepted )
            {
                if ( rel instanceof DependencyRelationship && !rel.isManaged() )
                {
                    final VersionlessArtifactRef key = new SimpleVersionlessArtifactRef( rel.getTargetArtifact() );
                    logger.debug( "Mapping existing dependency via key: {}", key );
                    concreteDeps.put( key, (DependencyRelationship) rel );
                }
            }

            calculateDependencyPluginPatch( depArtifactItems, concreteDeps, ref, pomView, result );
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( String.format( "Failed to build/query MavenPomView for: %s from: %s. Reason: %s", ref,
                                         locations, e.getMessage() ), e );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( String.format( "Failed to build/query MavenPomView for: %s from: %s. Reason: %s", ref,
                                         locations, e.getMessage() ), e );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( String.format( "Failed to build/query MavenPomView for: %s from: %s. Reason: %s", ref,
                                         locations, e.getMessage() ), e );
        }
    }

    private void calculateDependencyPluginPatch( final List<DependencyView> depArtifactItems,
                                                 final Map<VersionlessArtifactRef, DependencyRelationship> concreteDeps,
                                                 final ProjectVersionRef ref, final MavenPomView pomView,
                                                 final DiscoveryResult result )
    {
        logger.debug( "Detected {} dependency-plugin artifactItems that need to be accounted for in dependencies...",
                      depArtifactItems == null ? 0 : depArtifactItems.size() );
        if ( depArtifactItems != null && !depArtifactItems.isEmpty() )
        {
            final URI source = result.getSource();
            for ( final DependencyView depView : depArtifactItems )
            {
                try
                {
                    final URI pomLocation = RelationshipUtils.profileLocation( depView.getProfileId() );
                    final VersionlessArtifactRef depRef = depView.asVersionlessArtifactRef();
                    logger.debug( "Detected dependency-plugin usage with key: {}", depRef );

                    final DependencyRelationship dep = concreteDeps.get( depRef );
                    if ( dep != null )
                    {
                        if ( !DependencyScope.runtime.implies( dep.getScope() )
                            && ( dep.getPomLocation()
                                    .equals( pomLocation ) || dep.getPomLocation() == RelationshipUtils.POM_ROOT_URI ) )
                        {
                            logger.debug( "Correcting scope for: {}", dep );

                            if ( !result.removeDiscoveredRelationship( dep ) )
                            {
                                logger.error( "Failed to remove: {}", dep );
                            }

                            final Set<ProjectRef> excludes = dep.getExcludes();
                            final ProjectRef[] excludedRefs =
                                excludes == null ? new ProjectRef[0]
                                                : excludes.toArray( new ProjectRef[excludes.size()] );

                            final DependencyRelationship replacement =
                                new SimpleDependencyRelationship( dep.getSources(), ref, dep.getTargetArtifact(),
                                                            DependencyScope.embedded, dep.getIndex(), false,
                                                            depView.getOriginInfo().isInherited(),
                                                            excludedRefs );

                            if ( !result.addDiscoveredRelationship( replacement ) )
                            {
                                logger.error( "Failed to inject: {}", replacement );
                            }
                        }
                    }
                    else if ( depView.getVersion() != null )
                    {
                        logger.debug( "Injecting new dep: {}", depView.asArtifactRef() );
                        final DependencyRelationship injected =
                            new SimpleDependencyRelationship( source,
                                                        RelationshipUtils.profileLocation( depView.getProfileId() ),
                                                        ref, depView.asArtifactRef(), DependencyScope.embedded,
                                                        concreteDeps.size(), false, depView.getOriginInfo().isInherited() );

                        if ( !result.addDiscoveredRelationship( injected ) )
                        {
                            logger.error( "Failed to inject: {}", injected );
                        }
                    }
                    else
                    {
                        logger.error( "Invalid dependency referenced in artifactItems of dependency plugin configuration: {}. "
                                          + "No version was specified, and it does not reference an actual dependency.",
                                      depRef );
                    }
                }
                catch ( final GalleyMavenException e )
                {
                    logger.error( String.format( "Dependency is invalid: %s. Reason: %s. Skipping.", depView.toXML(),
                                                 e.getMessage() ), e );
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    logger.error( String.format( "Dependency is invalid: %s. Reason: %s. Skipping.", depView.toXML(),
                                                 e.getMessage() ), e );
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( String.format( "Dependency is invalid: %s. Reason: %s. Skipping.", depView.toXML(),
                                                 e.getMessage() ), e );
                }
            }
        }

    }

    @Override
    public String getId()
    {
        return "dependency-plugin";
    }

}
