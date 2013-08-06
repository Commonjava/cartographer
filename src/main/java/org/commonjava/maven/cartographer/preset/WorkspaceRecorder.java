package org.commonjava.maven.cartographer.preset;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;

public interface WorkspaceRecorder
{

    void save( GraphWorkspace workspace )
        throws CartoDataException;

}
