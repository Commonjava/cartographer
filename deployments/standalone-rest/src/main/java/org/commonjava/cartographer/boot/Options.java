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

import org.commonjava.cartographer.conf.CartographerConfig;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.deploy.undertow.UndertowBootOptions;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.Properties;

/**
 * Created by jdcasey on 4/19/16.
 */
public class Options
        extends BootOptions
        implements UndertowBootOptions
{
    public static final int DEFAULT_HTTP_PORT = 8082;

    private static final String DEFAULT_IP_BIND = "0.0.0.0";

    @Option( name = "port", aliases = {"-p"}, metaVar = "PORT", usage = "Bind the service to this port (default: 8082)" )
    private int port = DEFAULT_HTTP_PORT;

    @Option( name = "bind", aliases = {"-b", "--listen", "-l"}, metaVar = "IP_ADDR", usage = "Bind the service to this address (default: 0.0.0.0)" )
    private String bind = DEFAULT_IP_BIND;

    @Override
    public String getApplicationName()
    {
        return "Cartographer";
    }

    @Override
    public String getHomeSystemProperty()
    {
        return "carto.home";
    }

    @Override
    public String getConfigSystemProperty()
    {
        return "carto.conf";
    }

    @Override
    public String getHomeEnvar()
    {
        return "CARTO_HOME";
    }

    @Override
    public String getContextPath()
    {
        return "/";
    }

    @Override
    public String getDeploymentName()
    {
        return "Cartographer REST";
    }

    @Override
    public int getPort()
    {
        return port;
    }

    @Override
    public String getBind()
    {
        return bind;
    }

    public void setBind( String bind )
    {
        this.bind = bind;
    }

    @Override
    public void setPort( int port )
    {
        this.port = port;
    }

    @Override
    protected void setApplicationSystemProperties( Properties properties )
    {
        System.setProperty( CartographerConfig.CARTO_CONFIG_DIR_SYSPROP,
                            new File( getConfig() ).getParentFile().getAbsolutePath() );

        super.setApplicationSystemProperties( properties );
    }

    protected String getDefaultConfigFile()
    {
        return "/etc/cartographer/main.conf";
    }
}
