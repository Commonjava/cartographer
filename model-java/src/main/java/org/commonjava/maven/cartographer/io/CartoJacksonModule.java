package org.commonjava.maven.cartographer.io;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Created by jdcasey on 8/7/15.
 */
public class CartoJacksonModule
    extends SimpleModule
{
    public CartoJacksonModule()
    {
        super( "Serializers for Cartographer, including ProjectRef (and variants) and ProjectRelationship (and variants)" );

    }


}
