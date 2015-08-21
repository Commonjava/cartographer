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

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.request.MetadataCollationRequest;
import org.commonjava.cartographer.request.MetadataExtractionRequest;
import org.commonjava.cartographer.request.MetadataUpdateRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.result.MetadataCollationResult;
import org.commonjava.cartographer.result.MetadataResult;
import org.commonjava.cartographer.result.ProjectListResult;

/**
 * Created by jdcasey on 8/14/15.
 */
public interface MetadataOps
{
    MetadataResult getMetadata( MetadataExtractionRequest recipe )
                    throws CartoDataException, CartoRequestException;

    ProjectListResult updateMetadata( MetadataUpdateRequest recipe )
                    throws CartoDataException, CartoRequestException;

    ProjectListResult rescanMetadata( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    MetadataCollationResult collate( MetadataCollationRequest recipe )
                    throws CartoDataException, CartoRequestException;
}
