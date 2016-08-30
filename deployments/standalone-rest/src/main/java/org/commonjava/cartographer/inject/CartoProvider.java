/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.inject;

import org.commonjava.cartographer.conf.CartoDeploymentConfig;
import org.commonjava.cartographer.conf.CartographerConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class CartoProvider
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private CartoDeploymentConfig config;

    protected CartoProvider()
    {
    }

    @PostConstruct
    public void init()
    {
        this.config = new CartoDeploymentConfig();
    }

    @Produces
    @Default
    @ApplicationScoped
    public CartoDeploymentConfig getCartographerConfig()
    {
        return config;
    }

}
