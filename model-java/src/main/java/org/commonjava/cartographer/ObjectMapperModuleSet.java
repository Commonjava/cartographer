package org.commonjava.cartographer;

import com.fasterxml.jackson.databind.Module;

/**
 * Created by jdcasey on 8/24/16.
 */
public interface ObjectMapperModuleSet
{
    Iterable<Module> getSerializerModules();
}
