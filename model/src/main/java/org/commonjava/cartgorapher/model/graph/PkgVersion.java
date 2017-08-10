package org.commonjava.cartgorapher.model.graph;

/**
 * Created by jdcasey on 7/17/17.
 */
public final class PkgVersion
{
    private final PkgId packageId;

    private final String version;

    public PkgVersion( final PkgId packageId, final String version )
    {
        this.packageId = packageId;
        this.version = version;
    }

    public PkgId getPackageId()
    {
        return packageId;
    }

    public String getVersion()
    {
        return version;
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof PkgVersion ) )
        {
            return false;
        }

        final PkgVersion that = (PkgVersion) o;

        if ( !packageId.equals( that.packageId ) )
        {
            return false;
        }
        return version.equals( that.version );
    }

    @Override
    public int hashCode()
    {
        int result = packageId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
