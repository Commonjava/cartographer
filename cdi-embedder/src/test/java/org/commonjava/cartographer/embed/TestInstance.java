package org.commonjava.cartographer.embed;

import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by jdcasey on 8/30/16.
 */
@Target( {ElementType.METHOD} )
@Qualifier
public @interface TestInstance
{
}
