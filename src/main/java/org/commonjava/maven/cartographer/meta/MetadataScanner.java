package org.commonjava.maven.cartographer.meta;

import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

public interface MetadataScanner
{

    Map<String, String> scan( ProjectVersionRef ref, List<? extends Location> locations, Map<String, Object> context );

}
