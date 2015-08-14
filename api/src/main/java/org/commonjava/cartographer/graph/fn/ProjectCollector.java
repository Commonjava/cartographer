package org.commonjava.cartographer.graph.fn;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.CartoDataException;

public interface ProjectCollector<T>
{

    void accept( ProjectVersionRef ref, T result )
                    throws CartoRequestException, CartoDataException;

}
