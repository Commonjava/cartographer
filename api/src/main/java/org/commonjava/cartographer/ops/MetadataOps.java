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
