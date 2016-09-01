package org.commonjava.cartographer.conf;

import org.commonjava.propulsor.deploy.undertow.ui.UIConfiguration;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.annotation.PostConstruct;
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

    private UIConfiguration config = new UIConfiguration();

    public CartoUIConfig()
    {
        // default is disabled...
        config.setEnabled( false );
    }

    public File getUIDir()
    {
        return config.getUIDir();
    }

    @ConfigName( "ui.dir")
    public void setUIDir( File uiDir )
    {
        config.setUIDir( uiDir );
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
        return config;
    }

}
