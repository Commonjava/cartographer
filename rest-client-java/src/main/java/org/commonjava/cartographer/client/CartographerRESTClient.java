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
package org.commonjava.cartographer.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.commonjava.cartographer.CartoAPIObjectMapperModules;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.GraphCalculation;
import org.commonjava.cartographer.request.MetadataCollationRequest;
import org.commonjava.cartographer.request.MetadataExtractionRequest;
import org.commonjava.cartographer.request.MetadataUpdateRequest;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.request.MultiRenderRequest;
import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.cartographer.request.SingleGraphRequest;
import org.commonjava.cartographer.request.SourceAliasRequest;
import org.commonjava.cartographer.request.build.GraphAnalysisRequestBuilder;
import org.commonjava.cartographer.request.build.GraphCompositionBuilder;
import org.commonjava.cartographer.request.build.GraphDescriptionBuilder;
import org.commonjava.cartographer.request.build.MetadataCollationRequestBuilder;
import org.commonjava.cartographer.request.build.MetadataExtractionRequestBuilder;
import org.commonjava.cartographer.request.build.MetadataUpdateRequestBuilder;
import org.commonjava.cartographer.request.build.MultiGraphRequestBuilder;
import org.commonjava.cartographer.request.build.MultiRenderRequestBuilder;
import org.commonjava.cartographer.request.build.PathsRequestBuilder;
import org.commonjava.cartographer.request.build.PomRequestBuilder;
import org.commonjava.cartographer.request.build.ProjectGraphRelationshipsRequestBuilder;
import org.commonjava.cartographer.request.build.ProjectGraphRequestBuilder;
import org.commonjava.cartographer.request.build.RepositoryContentRequestBuilder;
import org.commonjava.cartographer.rest.dto.DownlogRequest;
import org.commonjava.cartographer.rest.dto.DownlogRequestBuilder;
import org.commonjava.cartographer.rest.dto.RepoContentResult;
import org.commonjava.cartographer.rest.dto.UrlMapResult;
import org.commonjava.cartographer.rest.dto.WorkspaceList;
import org.commonjava.cartographer.result.GraphDifference;
import org.commonjava.cartographer.result.GraphExport;
import org.commonjava.cartographer.result.MappedProjectRelationshipsResult;
import org.commonjava.cartographer.result.MappedProjectResult;
import org.commonjava.cartographer.result.MappedProjectsResult;
import org.commonjava.cartographer.result.MetadataCollationResult;
import org.commonjava.cartographer.result.MetadataResult;
import org.commonjava.cartographer.result.ProjectErrors;
import org.commonjava.cartographer.result.ProjectListResult;
import org.commonjava.cartographer.result.ProjectPathsResult;
import org.commonjava.cartographer.result.SourceAliasMapResult;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.propulsor.client.http.ClientHttpException;
import org.commonjava.propulsor.client.http.ClientHttpResponseErrorDetails;
import org.commonjava.propulsor.client.http.ClientHttpSupport;
import org.commonjava.propulsor.client.http.helper.HttpResources;
import org.commonjava.propulsor.client.http.helper.UrlUtils;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.ClientAuthenticator;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.PropertyAccessor.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;

/**
 * Created by jdcasey on 8/12/15.
 */
public class CartographerRESTClient
    implements AutoCloseable
{
    private final ClientHttpSupport http;

    public CartographerRESTClient( ClientHttpSupport http )
    {
        this.http = http;
    }

    public CartographerRESTClient( final String baseUrl, final PasswordManager passwordManager )
            throws ClientHttpException
    {
        this.http = new ClientHttpSupport( baseUrl, passwordManager, newObjectMapper() );
    }

    public CartographerRESTClient( final String baseUrl, final ClientAuthenticator authenticator )
            throws ClientHttpException
    {
        this.http = new ClientHttpSupport( baseUrl, authenticator, newObjectMapper() );
    }

    public CartographerRESTClient( final SiteConfig siteConfig, final HttpFactory httpFactory )
            throws ClientHttpException
    {
        this.http = new ClientHttpSupport( siteConfig, newObjectMapper(), httpFactory );
    }

    public ObjectMapper newObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules( new CartoAPIObjectMapperModules().getSerializerModules() );
        mapper.setVisibility(ALL, NONE);
        mapper.setVisibility(FIELD, ANY);
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public ClientHttpSupport getHttp()
    {
        return http;
    }

    public ObjectMapper getObjectMapper()
    {
        return getHttp().getObjectMapper();
    }

    public ProjectListResult list( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/project/list", request, ProjectListResult.class );
    }

    public MappedProjectResult parents( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/project/parents", request, MappedProjectResult.class );
    }

    public ProjectErrors errors( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/errors", request, ProjectErrors.class );
    }

    public ProjectListResult reindex( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/reindex", request, ProjectListResult.class );
    }

    public ProjectListResult incomplete( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/incomplete", request, ProjectListResult.class );
    }

    public ProjectListResult missing( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return incomplete( request );
    }

    public ProjectListResult variable( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/variable", request, ProjectListResult.class );
    }

    public ProjectPathsResult paths( PathsRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/paths", request, ProjectPathsResult.class );
    }

    public MappedProjectsResult ancestors( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/ancestry", request, MappedProjectsResult.class );
    }

    public BuildOrder buildOrder( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/build-order", request, BuildOrder.class );
    }

    public GraphExport graph( SingleGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/graph/export", request, GraphExport.class );
    }

    public GraphExport export( SingleGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return graph( request );
    }

    public MappedProjectRelationshipsResult relationshipsDeclaredBy( ProjectGraphRelationshipsRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/project/relationships", request,
                                           MappedProjectRelationshipsResult.class );
    }

    public MappedProjectRelationshipsResult relationshipsTargeting( ProjectGraphRelationshipsRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/project/targeting", request,
                                           MappedProjectRelationshipsResult.class );
    }

    public MetadataResult getMetadata( MetadataExtractionRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/meta", request, MetadataResult.class );
    }

    public ProjectListResult updateMetadata( MetadataUpdateRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/meta/updates", request, ProjectListResult.class );
    }

    public ProjectListResult rescanMetadata( ProjectGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/meta/rescan", request, ProjectListResult.class );
    }

    public MetadataCollationResult collateMetadata( MetadataCollationRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/meta/collation", request, MetadataCollationResult.class );
    }

    public void deleteWorkspace( String workspaceId )
            throws CartoClientException, ClientHttpException
    {
        getHttp().delete( UrlUtils.buildUrl( "depgraph/ws", workspaceId ) );
    }

    public WorkspaceList listWorkspaces()
            throws CartoClientException, ClientHttpException
    {
        return getHttp().get( "depgraph/ws", WorkspaceList.class );
    }

    public GraphDifference<ProjectVersionRef> calculateGraphDrift( GraphAnalysisRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/calc/drift", request,
                                           new TypeReference<GraphDifference<ProjectVersionRef>>()
                                           {
                                           } );
    }

    public GraphDifference<ProjectRelationship<?, ?>> graphDiff( GraphAnalysisRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/calc/drift", request,
                                           new TypeReference<GraphDifference<ProjectRelationship<?, ?>>>()
                                           {
                                           } );
    }

    public GraphCalculation calculate( MultiGraphRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/calc/drift", request, GraphCalculation.class );
    }

    public UrlMapResult repositoryUrlMap( RepositoryContentRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/repo/urlmap", request, UrlMapResult.class );
    }

    public RepoContentResult repositoryContent( RepositoryContentRequest request )
            throws CartoClientException, ClientHttpException
    {
        return getHttp().postWithResponse( "depgraph/repo/content", request, RepoContentResult.class );
    }

    public String repositoryDownloadLog( DownlogRequest request )
            throws CartoClientException, ClientHttpException
    {
        return postWithStringOutput( "depgraph/repo/downlog", request );
    }

    public InputStream repositoryZip( RepositoryContentRequest request )
            throws CartoClientException, IOException, ClientHttpException
    {
        HttpResources resources = getHttp().postRaw( "depgraph/repo/zip", request );
        if ( resources.getStatusCode() != HttpStatus.SC_OK )
        {
            throw new ClientHttpException( resources.getStatusCode(), "Error retrieving repo zip.\n%s",
                                            new ClientHttpResponseErrorDetails( resources.getResponse() ) );
        }

        return resources.getResponseEntityContent();
    }

    public String pom( PomRequest request )
            throws CartoClientException, ClientHttpException
    {
        return postWithStringOutput( "depgraph/render/pom", request );
    }

    public String dotfile( MultiRenderRequest request )
            throws CartoClientException, ClientHttpException
    {
        Map<String, String> params = request.getRenderParams();
        if ( params != null && !params.containsKey( "name" ) && !params.containsKey( "coord" ) )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn(
                    "You have not specified a 'name' or 'coord' parameter to be used in naming your Graphviz dotfile!" );
        }

        return postWithStringOutput( "depgraph/render/dotfile", request );
    }

    public String depTree( RepositoryContentRequest request )
            throws CartoClientException, ClientHttpException
    {
        return postWithStringOutput( "depgraph/render/depTree", request );
    }

    public String depList( RepositoryContentRequest request )
            throws CartoClientException, ClientHttpException
    {
        return postWithStringOutput( "depgraph/render/depList", request );
    }

    public ProjectGraphRequestBuilder newProjectGraphRequest()
    {
        return ProjectGraphRequestBuilder.newProjectGraphRequestBuilder();
    }

    public ProjectGraphRelationshipsRequestBuilder newProjectGraphRelationshipsRequest()
    {
        return ProjectGraphRelationshipsRequestBuilder.newProjectGraphRelationshipsRequestBuilder();
    }

    public MultiGraphRequestBuilder newMultiGraphRequest()
    {
        return MultiGraphRequestBuilder.newMultiGraphResolverRequestBuilder();
    }

    public PomRequestBuilder newPomRequest()
    {
        return PomRequestBuilder.newPomRequestBuilder();
    }

    public DownlogRequestBuilder newDownlogRequest()
    {
        return DownlogRequestBuilder.newDownlogRequestBuilder();
    }

    public GraphAnalysisRequestBuilder newGraphAnalysisRequest()
    {
        return GraphAnalysisRequestBuilder.newAnalysisRequestBuilder();
    }

    public MetadataExtractionRequestBuilder newMetadataExtractionRequest()
    {
        return MetadataExtractionRequestBuilder.newMetadataRecipeBuilder();
    }

    public MetadataUpdateRequestBuilder newMetadataUpdateRequest()
    {
        return MetadataUpdateRequestBuilder.newMetadataRecipeBuilder();
    }

    public MetadataCollationRequestBuilder newMetadataCollationRequest()
    {
        return MetadataCollationRequestBuilder.newMetadataRecipeBuilder();
    }

    public MultiRenderRequestBuilder newMultiRenderRequest()
    {
        return MultiRenderRequestBuilder.newMultiRenderRecipeBuilder();
    }

    public PathsRequestBuilder newPathsRequest()
    {
        return PathsRequestBuilder.newPathsRecipeBuilder();
    }

    public RepositoryContentRequestBuilder newRepositoryContentRequest()
    {
        return RepositoryContentRequestBuilder.newRepositoryContentRecipeBuilder();
    }

    public GraphDescriptionBuilder newGraphDescription()
    {
        return GraphDescriptionBuilder.newGraphDescriptionBuilder();
    }

    public GraphCompositionBuilder newGraphComposition()
    {
        return GraphCompositionBuilder.newGraphCompositionBuilder();
    }

    public Iterable<Module> getSerializerModules()
    {
        return new CartoAPIObjectMapperModules().getSerializerModules();
    }

    private String postWithStringOutput( String path, Object request )
            throws CartoClientException, ClientHttpException
    {
        String result = null;
        try (HttpResources resources = getHttp().postRaw( path, request ))
        {
            if ( resources.getStatusCode() != HttpStatus.SC_OK )
            {
                throw new ClientHttpException( resources.getStatusCode(), "Error retrieving response string.\n%s",
                                                new ClientHttpResponseErrorDetails( resources.getResponse() ) );
            }

            result = IOUtils.toString( resources.getResponseEntityContent() );
        }
        catch ( IOException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn( "Error closing response to path: " + path + ". Error: " + e.getMessage(), e );
        }

        return result;
    }

    @Override
    public void close()
            throws Exception
    {
        if ( http != null )
        {
            http.close();
        }
    }

    public boolean addSourceAlias( String alias, String url )
            throws ClientHttpException, CartoClientException
    {
        SourceAliasRequest req = new SourceAliasRequest( alias, url );
        Map<String, String> headers = new HashMap<>();
        headers.put( "Accept", "application/json" );
        headers.put( "Content-Type", "application/json" );

        try(HttpResources resources = http.postRaw( "admin/sources/aliases", req, headers ))
        {
            return resources.getResponse().getStatusLine().getStatusCode() == 200;
        }
        catch ( IOException e )
        {
            throw new CartoClientException( "Failed to add source alias. Reason: %s", e, e.getMessage() );
        }
    }

    public SourceAliasMapResult getSourceAliasMap()
            throws ClientHttpException
    {
        return http.get( "admin/sources/aliases", SourceAliasMapResult.class );
    }
}
