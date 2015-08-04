package org.commonjava.maven.cartographer.ops.fn;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface ProjectCollector<T>
{

    void accept( ProjectVersionRef ref, T result );

}
