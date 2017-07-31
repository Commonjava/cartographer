package org.commonjava.cartographer.boot;

import org.commonjava.propulsor.boot.Booter;

import static org.commonjava.propulsor.boot.BootOptions.BOOT_DEFAULTS_PROP;

/**
 * A Camel Application
 */
public class MainApp {

    private static final String CARTO_HOME = "carto.home";

    public static void main(String[] args)
    {
        String home = System.getProperty(CARTO_HOME);

        if ( home == null )
        {
            throw new IllegalArgumentException("VM argument ${carto.home} not found");
        }

        if ( System.getProperty(BOOT_DEFAULTS_PROP) == null )
        {
            System.setProperty(BOOT_DEFAULTS_PROP, home + "/bin/boot.properties");
        }

        Booter.main( args );
    }

}

