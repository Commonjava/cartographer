package org.commonjava.maven.cartographer.testutil;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.cartographer.event.CartoEventManagerImpl;

@ApplicationScoped
@TestData
public class TestCartoEventManager
    extends CartoEventManagerImpl
{

}
