package org.commonjava.cartographer.ops;

import org.commonjava.cartographer.CartoDataException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.galley.model.ConcreteResource;

import java.util.Map;

/**
 * Created by jdcasey on 8/14/15.
 */
public interface ResolveOps
{
    Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveRepositoryContents(
                    RepositoryContentRequest recipe )
                    throws CartoDataException, CartoRequestException;
}
