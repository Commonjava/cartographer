package org.commonjava.cartographer.data.model;

/**
 * Created by jdcasey on 7/17/17.
 */
public final class PkgId
{
    private final String packageType;

    private final String packageName;

    public PkgId( final String packageType, final String packageName )
    {
        this.packageType = packageType;
        this.packageName = packageName;
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof PkgId ) )
        {
            return false;
        }

        final PkgId pkgId = (PkgId) o;

        if ( !packageType.equals( pkgId.packageType ) )
        {
            return false;
        }
        return packageName.equals( pkgId.packageName );
    }

    @Override
    public int hashCode()
    {
        int result = packageType.hashCode();
        result = 31 * result + packageName.hashCode();
        return result;
    }

    public String getPackageType()
    {
        return packageType;
    }

    public String getPackageName()
    {
        return packageName;
    }
}
