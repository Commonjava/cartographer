package org.commonjava.maven.cartographer.preset;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.NoneFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

public class SOBBuildablesFilter
    implements ProjectRelationshipFilter
{

    private final ProjectRelationshipFilter filter;

    private final Map<ProjectRef, SingleVersion> selected;

    private final boolean acceptManaged;

    public SOBBuildablesFilter()
    {
        this( new HashMap<ProjectRef, SingleVersion>(), null );
    }

    public SOBBuildablesFilter( final boolean acceptManaged )
    {
        this.acceptManaged = acceptManaged;
        this.selected = new HashMap<>();
        this.filter =
            new OrFilter( new ParentFilter( false ), new DependencyFilter( DependencyScope.runtime, ScopeTransitivity.maven, false, true, true, null ) );
    }

    private SOBBuildablesFilter( final Map<ProjectRef, SingleVersion> selected, final ProjectRelationshipFilter childFilter )
    {
        this.selected = selected;
        this.acceptManaged = false;
        this.filter =
            childFilter == null ? new OrFilter( new ParentFilter( false ), new DependencyFilter( DependencyScope.runtime, ScopeTransitivity.maven,
                                                                                                 false, true, true, null ) ) : childFilter;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean result = false;

        if ( !acceptManaged && rel.isManaged() )
        {
            result = false;
        }
        else
        {
            result = filter.accept( rel );

            final ProjectVersionRef target = rel.getTarget();
            final ProjectRef targetGA = target.asProjectRef();
            if ( result && !selected.containsKey( targetGA ) && target.getVersionSpec()
                                                                      .isConcrete() )
            {
                selected.put( targetGA, (SingleVersion) target.getVersionSpec() );
            }
        }

        //        logger.info( "%s: accept(%s)", Boolean.toString( result )
        //                                              .toUpperCase(), rel );

        return result;
    }

    public Map<ProjectRef, SingleVersion> getSelectedProjectVersions()
    {
        return selected;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> lastRelationship )
    {
        switch ( lastRelationship.getType() )
        {
            case EXTENSION:
            case PLUGIN:
            case PLUGIN_DEP:
            {
                //                logger.info( "getChildFilter(%s)", lastRelationship );
                return new SOBBuildablesFilter( selected, new NoneFilter() );
            }
            case PARENT:
            {
                return this;
            }
            default:
            {
                //                logger.info( "getChildFilter(%s)", lastRelationship );

                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;
                if ( DependencyScope.test == dr.getScope() || DependencyScope.provided == dr.getScope() )
                {
                    return new SOBBuildablesFilter( selected, new NoneFilter() );
                }

                return new SOBBuildablesFilter( selected, filter.getChildFilter( lastRelationship ) );
            }
        }
    }

    @Override
    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "Shipping-Oriented Builds Filter (sub-filter=" );

        if ( filter == null )
        {
            sb.append( "NONE" );
        }
        else
        {
            filter.render( sb );
        }

        sb.append( " [acceptManaged=" )
          .append( acceptManaged )
          .append( "])" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
