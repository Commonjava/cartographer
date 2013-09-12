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
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.reader.MavenPomReader;
import org.commonjava.maven.galley.maven.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.util.logging.Logger;
import org.w3c.dom.Element;

public class DistributionPomPatcher
    implements DepgraphPatcher
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private MavenPomReader pomReader;

    protected DistributionPomPatcher()
    {
    }

    public DistributionPomPatcher( final MavenPomReader pomReader )
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
            final MavenPomView pomView = pomReader.read( ref, locations );

            final Set<ProjectRelationship<?>> accepted = new HashSet<>( orig.getAcceptedRelationships() );
            final Set<ProjectRelationship<?>> rejected = new HashSet<>( orig.getRejectedRelationships() );

            final Map<ProjectVersionRef, DependencyRelationship> concreteDeps = new HashMap<>();
            for ( final ProjectRelationship<?> rel : accepted )
            {
                if ( rel instanceof DependencyRelationship && !rel.isManaged() )
                {
                    concreteDeps.put( rel.getTarget()
                                         .asProjectVersionRef(), (DependencyRelationship) rel );
                }
            }

            calculatePomWithAssembliesPatch( concreteDeps, ref, pomView, orig.getSource(), accepted, rejected );

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

    private void calculatePomWithAssembliesPatch( final Map<ProjectVersionRef, DependencyRelationship> concreteDeps, final ProjectVersionRef ref,
                                                  final MavenPomView pomView, final URI source, final Set<ProjectRelationship<?>> accepted,
                                                  final Set<ProjectRelationship<?>> rejected )
    {
        // TODO: find a way to detect an assembly/distro pom, and turn deps from provided scope to compile scope.
        final String assemblyOnPomProjectPath =
            "/project[packaging/text()=\"pom\"]//plugin[artifactId/text()=\"maven-assembly-plugin\"]//configuration[appendAssemblyId/text()=\"false\"]";

        final Element assemblyConfigTest = pomView.resolveXPathToElement( assemblyOnPomProjectPath, false );
        if ( assemblyConfigTest != null )
        {
            logger.info( "Detected pom-packaging project with an assembly that produces artifacts without classifiers..."
                + "Need to flip provided-scope deps to compile scope here." );

            // flip provided scope to compile scope...is this dangerous??
            // if the project has packaging == pom and a series of assemblies, it's likely to be a distro project, and not meant for consumption via maven
            // also, if something like type == zip is used for a dependency, IIRC transitive deps are not traversed during the build.
            // so this SHOULD be safe.
            for ( final DependencyRelationship dep : concreteDeps.values() )
            {
                if ( dep.getScope() == DependencyScope.provided )
                {
                    logger.info( "Fixing scope for: %s", dep );

                    accepted.remove( dep );
                    final Set<ProjectRef> excludes = dep.getExcludes();
                    final ProjectRef[] excludedRefs = excludes == null ? new ProjectRef[0] : excludes.toArray( new ProjectRef[excludes.size()] );

                    final DependencyRelationship replacement =
                        new DependencyRelationship( dep.getSources(), ref, dep.getTargetArtifact(), DependencyScope.embedded, dep.getIndex(), false,
                                                    excludedRefs );

                    accepted.add( replacement );
                }
            }
        }
    }

    @Override
    public String getId()
    {
        return "dist-pom";
    }

}
