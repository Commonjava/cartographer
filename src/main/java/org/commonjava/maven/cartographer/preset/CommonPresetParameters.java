package org.commonjava.maven.cartographer.preset;

import java.util.Map;

import org.commonjava.maven.atlas.ident.DependencyScope;

public final class CommonPresetParameters
{

    public static final String SCOPE = "scope";

    public static final String MANAGED = "managed";

    private CommonPresetParameters()
    {
    }

    public static void coerce( final Map<String, Object> params )
    {
        Object scope = params.remove( SCOPE );
        if ( scope != null )
        {
            if ( !( scope instanceof DependencyScope ) )
            {
                scope = DependencyScope.getScope( scope.toString() );
            }

            params.put( SCOPE, scope );
        }

        Object managed = params.remove( MANAGED );
        if ( managed != null )
        {
            if ( !( managed instanceof Boolean ) )
            {
                managed = Boolean.valueOf( managed.toString() );
            }

            params.put( MANAGED, managed );
        }
    }

}
