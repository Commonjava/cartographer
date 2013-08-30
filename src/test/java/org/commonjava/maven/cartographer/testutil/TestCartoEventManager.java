package org.commonjava.maven.cartographer.testutil;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.cartographer.event.CartoEventManagerImpl;
import org.commonjava.maven.galley.testing.core.cdi.TestData;

@ApplicationScoped
@TestData
public class TestCartoEventManager
    extends CartoEventManagerImpl
{

}
