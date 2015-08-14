package org.commonjava.cartographer.ops;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.result.*;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.request.SingleGraphRequest;

/**
 * Created by jdcasey on 8/14/15.
 */
public interface GraphOps
{
    ProjectListResult listProjects( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    /**
     * Lists all paths leading from roots defined in request to target projects for the configured graph composition.
     *
     * @param recipe the graph request
     * @return the list of paths, each path is a list of project relationships
     */
    ProjectPathsResult getPaths( PathsRequest recipe )
                    throws CartoDataException, CartoRequestException;

    ProjectErrors getProjectErrors( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    MappedProjectResult getProjectParent( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    MappedProjectRelationshipsResult getDirectRelationshipsFrom( ProjectGraphRelationshipsRequest recipe )
                    throws CartoDataException, CartoRequestException;

    MappedProjectRelationshipsResult getDirectRelationshipsTo( ProjectGraphRelationshipsRequest recipe )
                    throws CartoDataException, CartoRequestException;

    ProjectListResult reindex( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    ProjectListResult getIncomplete( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    ProjectListResult getVariable( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    MappedProjectsResult getAncestry( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    BuildOrder getBuildOrder( ProjectGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;

    GraphExport exportGraph( SingleGraphRequest recipe )
                    throws CartoDataException, CartoRequestException;
}
