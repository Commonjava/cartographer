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

import org.commonjava.cartographer.Cartographer;
import org.commonjava.maven.cartographer.ftest.testutil.DockerDriven;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.util.Map;

import static org.commonjava.maven.cartographer.ftest.testutil.TestFileUtils.writeConfigFile;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Using an external Indy instance (Docker container), start Cartographer and point it at the
 * Indy instance. Verify that the new Cartographer instance aliases the Indy repositories / groups on startup.
 */
@Category( DockerDriven.class )
public class IndyEndpointAliasingTest
{

    public static final java.lang.String INDY_HOST_PROP = "indy.host";

    private static final java.lang.String INDY_PORT_PROP = "indy.port";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private LocalTestDriver driver;

    protected Cartographer carto;

    private String indyUrl;

    @Before
    public void before()
            throws Exception
    {
        String indyHost = System.getProperty( INDY_HOST_PROP );
        String indyPort = System.getProperty( INDY_PORT_PROP );

        indyUrl = String.format( "http://%s:%s/api", indyHost, indyPort );

        driver = new LocalTestDriver();
        driver.setConfigurator( (configDir)->{
            writeConfigFile( configDir, LocalTestDriver.MAIN_CONF, "[aliases]\nindy.url=" + indyUrl );
        } );

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

    @Test
    public void run()
        throws Exception
    {
        Map<String, String> aliasMap = carto.getSourceAliasMap();
        System.out.println("Got aliases:\n");
        aliasMap.forEach( ( alias, url ) -> System.out.printf( "  %s => %s\n", alias, url ) );

        assertThat( "Central repo from Indy was not aliased!", aliasMap.containsKey( "remote:central" ) );
        assertThat( "Public group from Indy was not aliased!", aliasMap.containsKey( "group:public" ) );
        assertThat( "Local-Deployments hosted repo from Indy was not aliased!", aliasMap.containsKey( "hosted:local-deployments" ) );
    }

}
