package org.commonjava.maven.cartographer.meta;

import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface MetadataScanner
{

    Map<String, String> scan( ProjectVersionRef ref, Map<String, Object> scannerCache );

}
