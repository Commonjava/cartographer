package org.commonjava.maven.cartographer.discover.patch;

import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.ProjectRefView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Rule;

public abstract class AbstractPatcherTest
{

    @Rule
    public GalleyMavenFixture galleyFixture = new GalleyMavenFixture( new CoreFixture() );

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    protected void setupGalley()
    {
        galleyFixture.initMissingComponents();
    }

    protected Map<String, Object> getContext( final ProjectVersionRef ref, final Location location )
        throws Exception
    {
        final Transfer txfr = galleyFixture.getArtifacts()
                                           .retrieve( location, ref.asPomArtifact() );
        final MavenPomView read = galleyFixture.getPomReader()
                                               .read( ref, txfr, Arrays.asList( location ) );

        final Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put( POM_VIEW_CTX_KEY, read );
        ctx.put( TRANSFER_CTX_KEY, txfr );

        return ctx;
    }

    protected Set<ProjectRelationship<?>> parseDependencyRelationships( final String pom, final ProjectVersionRef pvr, final Location location,
                                                                        final URI src )
        throws GalleyMavenException
    {
        final Set<ProjectRelationship<?>> discovered = new HashSet<ProjectRelationship<?>>();
        final MavenPomView pomView = galleyFixture.getPomReader()
                                                  .read( pvr, Arrays.asList( location ) );
        List<DependencyView> deps = pomView.getAllDirectDependencies();
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

            final URI pomLoc = RelationshipUtils.profileLocation( dep.getProfileId() );

            discovered.add( new DependencyRelationship( src, pomLoc, pvr, dep.asArtifactRef(), dep.getScope(), idx++, false, depEx ) );
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

            final URI pomLoc = RelationshipUtils.profileLocation( dep.getProfileId() );

            discovered.add( new DependencyRelationship( src, pomLoc, pvr, dep.asArtifactRef(), dep.getScope(), idx++, true, depEx ) );
        }

        return discovered;
    }

}
