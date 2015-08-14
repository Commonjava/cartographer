package org.commonjava.cartographer.request;

import java.util.Set;

public class MetadataExtractionRequest
    extends ProjectGraphRequest
{

    private Set<String> keys;

    public Set<String> getKeys()
    {
        return keys;
    }

    public void setKeys( final Set<String> keys )
    {
        this.keys = keys;
    }

}
