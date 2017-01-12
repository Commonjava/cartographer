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
package org.commonjava.cartographer.conf;

import org.commonjava.propulsor.deploy.undertow.ui.UIConfiguration;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.File;

/**
 * Created by jdcasey on 9/1/16.
 */
@SectionName("ui")
@ApplicationScoped
public class CartoUIConfig
    implements CartoSubConfig
{

    public static final File DEFAULT_UI_DIR = new File( System.getProperty( "carto.home", "/" ), "var/lib/cartographer/ui" );

    private UIConfiguration config = new UIConfiguration();

    public CartoUIConfig() {}

    public File getUIDir() { return config.getUIDir(); }

    @ConfigName( "ui.dir")
    public void setUIDir( final File uiDir )
    {
        config.setUIDir(uiDir);
    }

    @ConfigName( "enabled" )
    public void setEnabled( boolean enabled )
    {
        config.setEnabled( enabled );
    }

    public boolean isEnabled()
    {
        return config.isEnabled();
    }

    @Produces
    @Default
    @ApplicationScoped
    public UIConfiguration getUiConfiguration()
    {
        if ( config.getUIDir() == null )
        {
            config.setUIDir( DEFAULT_UI_DIR );
        }
        return config;
    }

}
