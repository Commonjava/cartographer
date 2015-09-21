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
package org.commonjava.cartographer.request.build;

import org.commonjava.cartographer.request.GraphDescription;
import org.commonjava.cartographer.request.MetadataUpdateRequest;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MetadataUpdateRequestBuilder<T extends MetadataUpdateRequestBuilder<T, R>, R extends MetadataUpdateRequest>
    extends ProjectGraphRequestBuilder<T, R>
{

    public static final class StandaloneMeta
        extends MetadataUpdateRequestBuilder<StandaloneMeta, MetadataUpdateRequest>
    {
    }

    public static StandaloneMeta newMetadataRecipeBuilder()
    {
        return new StandaloneMeta();
    }

    private Map<String, String> globalMetadata;

    private Map<ProjectVersionRef, Map<String, String>> projectMetadata;

    public T withGlobalMetadata(Map<String, String> metadata)
    {
        this.globalMetadata = metadata;
        return self;
    }

    public synchronized T withGlobalMetadata( String key, String value )
    {
        if ( globalMetadata == null )
        {
            globalMetadata = new HashMap<>();
        }
        globalMetadata.put( key, value );
        return self;
    }

    public T withProjectMetadata( Map<ProjectVersionRef, Map<String, String>> projectMetadata)
    {
        this.projectMetadata = projectMetadata;
        return self;
    }

    public synchronized T withProjectMetadata( ProjectVersionRef project, Map<String, String> metadata)
    {
        if ( projectMetadata == null )
        {
            projectMetadata = new HashMap<>();
        }
        projectMetadata.put(project, metadata);
        return self;
    }

    public synchronized T withProjectMetadata( ProjectVersionRef project, String key, String value)
    {
        if ( projectMetadata == null )
        {
            projectMetadata = new HashMap<>();
        }

        Map<String, String> metadata = projectMetadata.get( project );
        if ( metadata == null )
        {
            metadata = new HashMap<>();
            projectMetadata.put( project, metadata );
        }

        metadata.put(key, value);
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new MetadataUpdateRequest();
        configure( recipe );
        recipe.setGlobalMetadata( globalMetadata );
        recipe.setProjectMetadata( projectMetadata );

        return recipe;
    }

    @Override
    public T withTargetProject( ProjectVersionRef project )
    {
        return super.withTargetProject( project );
    }

    @Override
    public T withProjectGavPattern( String projectGavPattern )
    {
        return super.withProjectGavPattern( projectGavPattern );
    }

    @Override
    public T withProjectVersionRef( ProjectVersionRef ref )
    {
        return super.withProjectVersionRef( ref );
    }

    @Override
    public T withGraph( GraphDescription graph )
    {
        return super.withGraph( graph );
    }

    @Override
    public T withSource( String source )
    {
        return super.withSource( source );
    }

    @Override
    public T withWorkspaceId( String workspaceId )
    {
        return super.withWorkspaceId( workspaceId );
    }

    @Override
    public T withSourceLocation( Location source )
    {
        return super.withSourceLocation( source );
    }

    @Override
    public T withTimeoutSecs( Integer timeoutSecs )
    {
        return super.withTimeoutSecs( timeoutSecs );
    }

    @Override
    public T withPatcherIds( Collection<String> patcherIds )
    {
        return super.withPatcherIds( patcherIds );
    }

    @Override
    public T withResolve( boolean resolve )
    {
        return super.withResolve( resolve );
    }

    @Override
    public T withInjectedBOMs( List<ProjectVersionRef> injectedBOMs )
    {
        return super.withInjectedBOMs( injectedBOMs );
    }

    @Override
    public T withExcludedSubgraphs( Collection<ProjectVersionRef> excludedSubgraphs )
    {
        return super.withExcludedSubgraphs( excludedSubgraphs );
    }

    @Override
    public T withVersionSelections( Map<ProjectRef, ProjectVersionRef> versionSelections )
    {
        return super.withVersionSelections( versionSelections );
    }
}
