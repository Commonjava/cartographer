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
package org.commonjava.cartographer.boot;

import org.commonjava.propulsor.boot.Booter;

import static org.commonjava.propulsor.boot.BootOptions.BOOT_DEFAULTS_PROP;

/**
 * Main class for Cartographer standalone application.
 *
 * Created by ruhan on 1/3/17.
 */
public class Main {

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

        Booter.main(args);
    }
}
