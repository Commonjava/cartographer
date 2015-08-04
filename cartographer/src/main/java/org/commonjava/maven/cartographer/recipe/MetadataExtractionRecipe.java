package org.commonjava.maven.cartographer.recipe;

import java.util.Set;

public class MetadataExtractionRecipe
    extends ProjectGraphRecipe
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
