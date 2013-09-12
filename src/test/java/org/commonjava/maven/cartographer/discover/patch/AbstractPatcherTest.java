package org.commonjava.maven.cartographer.discover.patch;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.view.DependencyView;
import org.commonjava.maven.galley.maven.view.MavenPomView;
import org.commonjava.maven.galley.maven.view.ProjectRefView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Rule;

public abstract class AbstractPatcherTest
{

    @Rule
    public CoreFixture galleyCore = new CoreFixture();

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    protected void setupGalley()
    {
        galleyCore.initMissingComponents();
    }

    protected Set<ProjectRelationship<?>> parseDependencyRelationships( final String pom, final ProjectVersionRef pvr, final Location location,
                                                                        final URI src )
    {
        final Set<ProjectRelationship<?>> discovered = new HashSet<>();
        final MavenPomView pomView = galleyCore.getPomReader()
                                               .read( pvr, Arrays.asList( location ) );
        List<DependencyView> deps = pomView.getAllDependencies();
        int idx = 0;
        for ( final DependencyView dep : deps )
        {
            final Set<ProjectRefView> depExclusions = dep.getExclusions();
            final ProjectRef[] depEx;
            if ( depExclusions != null )
            {
                depEx = new ProjectRef[depExclusions.size()];
                int i = 0;
                for ( final ProjectRefView ex : depExclusions )
                {
                    depEx[i++] = ex.asProjectRef();
                }
            }
            else
            {
                depEx = new ProjectRef[0];
            }

            discovered.add( new DependencyRelationship( src, pvr, dep.asArtifactRef(), dep.getScope(), idx++, false, depEx ) );
        }

        deps = pomView.getAllManagedDependencies();
        idx = 0;
        for ( final DependencyView dep : deps )
        {
            final Set<ProjectRefView> depExclusions = dep.getExclusions();
            final ProjectRef[] depEx;
            if ( depExclusions != null )
            {
                depEx = new ProjectRef[depExclusions.size()];
                int i = 0;
                for ( final ProjectRefView ex : depExclusions )
                {
                    depEx[i++] = ex.asProjectRef();
                }
            }
            else
            {
                depEx = new ProjectRef[0];
            }

            discovered.add( new DependencyRelationship( src, pvr, dep.asArtifactRef(), dep.getScope(), idx++, true, depEx ) );
        }

        return discovered;
    }

}
