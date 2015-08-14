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
package org.commonjava.maven.cartographer.ftest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.maven.galley.util.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractCartographerTCK
{

    protected static final String GRAPHS = "graphs";

    protected static final String DTOS = "dto";

    protected static final String OUTPUT = "output";

    protected abstract String getTestDir();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private CartoTCKDriver driver;

    protected Cartographer carto;

    @Before
    public void before()
        throws Exception
    {
        final ServiceLoader<CartoTCKDriver> driverLoader = ServiceLoader.load( CartoTCKDriver.class );
        final Iterator<CartoTCKDriver> driverIter = driverLoader.iterator();
        if ( !driverIter.hasNext() )
        {
            throw new IllegalStateException( "No TCK driver found!" );
        }

        driver = driverIter.next();

        carto = driver.start( temp );
    }

    @After
    public void after()
        throws Exception
    {
        if ( driver != null )
        {
            driver.stop();
        }
    }

    protected void aliasRepo( final String alias, final String repoResource, final int repoResourceTrim )
        throws Exception
    {
        String path = resourcePath( GRAPHS, getTestDir(), repoResource );

        final URL pomUrl = Thread.currentThread()
                                 .getContextClassLoader()
                                 .getResource( path );

        assertThat( repoResource + " is not on the classpath!", pomUrl, notNullValue() );

        File f = new File( pomUrl.getPath() );
        for ( int i = 0; i < repoResourceTrim; i++ )
        {
            f = f.getParentFile();
        }

        System.out.println( "Got file: " + f );

        path = f.getPath();
        if ( path.contains( ".jar!" ) )
        {
            path = "jar:" + path;
        }
        else
        {
            path = "file:" + path;
        }

        System.out.println( "Got repo path: " + path );

        driver.createRepoAlias( alias, path );
    }

    protected <T> T readRecipe( final String dtoFile, final Class<T> type )
        throws Exception
    {
        final String dto = resourcePath( GRAPHS, getTestDir(), DTOS, dtoFile );
        final InputStream dtoStream = Thread.currentThread()
                                            .getContextClassLoader()
                                            .getResourceAsStream( dto );

        assertThat( dto + " is not on the classpath!", dtoStream, notNullValue() );

        try
        {
            return carto.getObjectMapper()
                        .readValue( dtoStream, type );
        }
        finally
        {
            IOUtils.closeQuietly( dtoStream );
        }
    }

    protected String resourcePath( final String... parts )
    {
        return PathUtils.normalize( parts );
    }

    protected void assertPomDeps( final Model pom, final boolean managed, final String depsFile )
        throws Exception
    {
        final String depListing = resourcePath( GRAPHS, getTestDir(), OUTPUT, depsFile );

        final InputStream depsStream = Thread.currentThread()
                                             .getContextClassLoader()
                                             .getResourceAsStream( depListing );

        final List<String> specs = IOUtils.readLines( depsStream );

        System.out.println( "Asserting presence of artifacts:\n  " + StringUtils.join( specs, "\n  " ) );

        IOUtils.closeQuietly( depsStream );

        final List<Dependency> deps;
        if ( managed )
        {
            final DependencyManagement dm = pom.getDependencyManagement();
            assertThat( dm, notNullValue() );
            deps = dm.getDependencies();
        }
        else
        {
            deps = pom.getDependencies();
        }

        final List<ArtifactRef> depArtifacts = new ArrayList<ArtifactRef>();
        for ( final Dependency dep : deps )
        {
            final String depSpec =
                String.format( "%s:%s:%s:%s%s", dep.getGroupId(), dep.getArtifactId(), dep.getVersion(),
                               ( dep.getType() == null ? "jar" : dep.getType() ), ( dep.getClassifier() == null ? ""
                                               : ":" + dep.getClassifier() ) );

            depArtifacts.add( ArtifactRef.parse( depSpec ) );
        }

        System.out.println( "POM dependencies:\n  " + StringUtils.join( depArtifacts, "\n  " ) );

        assertThat( depArtifacts.size(), equalTo( specs.size() ) );
        for ( final String spec : specs )
        {
            final ArtifactRef ar = ArtifactRef.parse( spec );
            assertThat( spec + " was missing from dependencies!", depArtifacts.contains( ar ), equalTo( true ) );
        }
    }
}
