package org.commonjava.maven.cartographer.discover.patch;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.reader.MavenPomReader;
import org.commonjava.maven.galley.maven.view.DependencyView;
import org.commonjava.maven.galley.maven.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.util.logging.Logger;
import org.w3c.dom.Element;

public class DependencyPluginPatcher
    implements DepgraphPatcher
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private MavenPomReader pomReader;

    protected DependencyPluginPatcher()
    {
    }

    public DependencyPluginPatcher( final MavenPomReader pomReader )
    {
        this.pomReader = pomReader;
    }

    @Override
    public DiscoveryResult patch( final DiscoveryResult orig, final List<? extends Location> locations, final Map<String, Object> context )
    {
        final DiscoveryResult result = orig;
        final ProjectVersionRef ref = result.getSelectedRef();
        try
        {
            MavenPomView pomView = (MavenPomView) context.get( POM_VIEW );
            if ( pomView == null )
            {
                pomView = pomReader.read( ref, locations );
                context.put( POM_VIEW, pomView );
            }

            // get all artifactItems with a version element. Only these need to be verified.
            final String depArtifactItemsPath = "//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem";

            logger.info( "Looking for dependency-plugin usages matching: '%s'", depArtifactItemsPath );
            final List<Element> depArtifactItems = pomView.resolveXPathToElements( depArtifactItemsPath, false );
            if ( depArtifactItems == null || depArtifactItems.isEmpty() )
            {
                return orig;
            }

            final Set<ProjectRelationship<?>> accepted = new HashSet<>( orig.getAcceptedRelationships() );
            final Set<ProjectRelationship<?>> rejected = new HashSet<>( orig.getRejectedRelationships() );

            final Map<VersionlessArtifactRef, DependencyRelationship> concreteDeps = new HashMap<>();
            for ( final ProjectRelationship<?> rel : accepted )
            {
                if ( rel instanceof DependencyRelationship && !rel.isManaged() )
                {
                    final VersionlessArtifactRef key = new VersionlessArtifactRef( rel.getTargetArtifact() );
                    logger.info( "Mapping existing dependency via key: %s", key );
                    concreteDeps.put( key, (DependencyRelationship) rel );
                }
            }

            calculateDependencyPluginPatch( depArtifactItems, concreteDeps, ref, pomView, orig.getSource(), accepted, rejected );

            final Set<ProjectRelationship<?>> all = new HashSet<>();
            all.addAll( accepted );
            all.addAll( rejected );

            return new DiscoveryResult( orig.getSource(), ref, all, rejected );
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( "Failed to build/query MavenPomView for: %s from: %s. Reason: %s", e, ref, locations, e.getMessage() );
        }

        return result;
    }

    private void calculateDependencyPluginPatch( final List<Element> depArtifactItems,
                                                 final Map<VersionlessArtifactRef, DependencyRelationship> concreteDeps, final ProjectVersionRef ref,
                                                 final MavenPomView pomView, final URI source, final Set<ProjectRelationship<?>> accepted,
                                                 final Set<ProjectRelationship<?>> rejected )
    {
        logger.info( "Detected %d dependency-plugin artifactItems that need to be accounted for in dependencies...", depArtifactItems == null ? 0
                        : depArtifactItems.size() );
        if ( depArtifactItems != null && !depArtifactItems.isEmpty() )
        {
            for ( final Element ai : depArtifactItems )
            {
                final URI pomLocation = RelationshipUtils.profileLocation( pomView.getProfileIdFor( ai ) );
                final DependencyView depView = pomView.asDependency( ai );
                final VersionlessArtifactRef depRef = depView.asVersionlessArtifactRef();
                logger.info( "Detected dependency-plugin usage with key: %s", depRef );

                final DependencyRelationship dep = concreteDeps.get( depRef );
                if ( dep != null )
                {
                    if ( !DependencyScope.runtime.implies( dep.getScope() )
                        && ( dep.getPomLocation()
                                .equals( pomLocation ) || dep.getPomLocation() == RelationshipUtils.POM_ROOT_URI ) )
                    {
                        logger.info( "Correcting scope for: %s", dep );
                        accepted.remove( dep );
                        final Set<ProjectRef> excludes = dep.getExcludes();
                        final ProjectRef[] excludedRefs = excludes == null ? new ProjectRef[0] : excludes.toArray( new ProjectRef[excludes.size()] );

                        final DependencyRelationship replacement =
                            new DependencyRelationship( dep.getSources(), ref, dep.getTargetArtifact(), DependencyScope.embedded, dep.getIndex(),
                                                        false, excludedRefs );

                        accepted.add( replacement );
                    }
                }
                else if ( depView.getVersion() != null )
                {
                    logger.info( "Injecting new dep: %s", depView.asArtifactRef() );
                    final DependencyRelationship replacement =
                        new DependencyRelationship( source, RelationshipUtils.profileLocation( depView.getProfileId() ), ref,
                                                    depView.asArtifactRef(), DependencyScope.embedded, concreteDeps.size(), false );

                    accepted.add( replacement );
                }
                else
                {
                    logger.error( "Invalid dependency referenced in artifactItems of dependency plugin configuration: %s. "
                        + "No version was specified, and it does not reference an actual dependency.", depRef );
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
