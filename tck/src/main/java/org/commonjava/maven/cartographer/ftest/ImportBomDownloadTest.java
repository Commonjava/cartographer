package org.commonjava.maven.cartographer.ftest;

import org.apache.maven.model.Model;
import org.commonjava.maven.cartographer.dto.PomRecipe;
import org.junit.Test;

public class ImportBomDownloadTest
    extends AbstractCartographerTCK
{

    private static final String PROJECT = "import-bom";

    @Test
    public void run()
        throws Exception
    {
        final String dto = "pom.json";
        final String depsTxt = "deps.txt";
        final String repoResource = "/repo/org/foo/consumer/1/consumer-1.pom";
        final int repoResourceTrim = 5;
        final String alias = "test";

        aliasRepo( alias, repoResource, repoResourceTrim );

        final PomRecipe recipe = readRecipe( dto, PomRecipe.class );

        final Model pom = carto.getRenderer()
                               .generatePOM( recipe );

        assertPomDeps( pom, false, depsTxt );
    }

    @Override
    protected String getTestDir()
    {
        return PROJECT;
    }

}
