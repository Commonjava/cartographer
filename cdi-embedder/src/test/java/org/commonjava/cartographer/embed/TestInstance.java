package org.commonjava.cartographer.embed;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.inject.Named;
import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Created by jdcasey on 8/30/16.
 */
@Stereotype
@Named
@Qualifier
@Alternative
@Retention( RetentionPolicy.RUNTIME )
@Target( { METHOD, FIELD, TYPE, PARAMETER } )
public @interface TestInstance
{
}
