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
package org.commonjava.cartographer.ops;

import org.apache.maven.model.Model;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.atlas.graph.traverse.print.StructureRelationshipPrinter;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.request.MultiRenderRequest;
import org.commonjava.cartographer.request.PomRequest;

import java.io.PrintWriter;

/**
 * Created by jdcasey on 8/14/15.
 */
public interface GraphRenderingOps
{
    void depTree( RepositoryContentRequest recipe, boolean collapseTransitives, PrintWriter writer )
                    throws CartoDataException, CartoRequestException;

    void depTree( RepositoryContentRequest recipe, boolean collapseTransitives, StructureRelationshipPrinter relPrinter,
                  PrintWriter writer )
                    throws CartoDataException, CartoRequestException;

    void depList( RepositoryContentRequest recipe, PrintWriter writer )
                    throws CartoDataException, CartoRequestException;

    void depList( RepositoryContentRequest recipe, StructureRelationshipPrinter relPrinter, PrintWriter writer )
                    throws CartoDataException, CartoRequestException;

    @SuppressWarnings( "null" )
    Model generatePOM( PomRequest recipe )
                    throws CartoDataException, CartoRequestException;

    String dotfile( MultiRenderRequest recipe )
                    throws CartoDataException, CartoRequestException;
}
