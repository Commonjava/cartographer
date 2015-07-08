package org.commonjava.maven.cartographer.ftest;

import org.commonjava.maven.cartographer.Cartographer;
import org.junit.rules.TemporaryFolder;

public interface CartoTCKDriver
{

    Cartographer start( TemporaryFolder temp )
        throws Exception;

    void stop()
        throws Exception;

    void createRepoAlias( String alias, String repoResource )
        throws Exception;

}
