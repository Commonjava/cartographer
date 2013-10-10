package org.commonjava.maven.cartographer.dto;

import java.util.Set;

public class MetadataCollationRecipe
    extends ResolverRecipe
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

    @Override
    public void normalize()
    {
        super.normalize();
        normalize( keys );
    }

}
