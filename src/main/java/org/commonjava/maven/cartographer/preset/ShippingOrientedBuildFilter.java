package org.commonjava.maven.cartographer.preset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.util.logging.Logger;

//FIXME: Find a way to store selections appropriately in depgraph. BUT, they have to be isolately appropriately to classloader...
public class ShippingOrientedBuildFilter
    implements ProjectRelationshipFilter
{

    private final Logger logger = new Logger( getClass() );

    private final boolean runtimeOnly;

    private final ProjectRelationshipFilter filter;

    private final Set<ProjectRef> excludes = new HashSet<ProjectRef>();

    private final Map<ProjectRef, SingleVersion> selected;

    public ShippingOrientedBuildFilter()
    {
        this.runtimeOnly = false;
        this.filter = null;
        this.selected = new HashMap<>();
    }

    private ShippingOrientedBuildFilter( final boolean runtimeOnly, final Set<ProjectRef> excludes,
                                         final Map<ProjectRef, SingleVersion> selected )
    {
        //        logger.info( "Creating filter %s",
        //                     runtimeOnly ? "for runtime artifacts ONLY - only dependencies in the runtime/compile scope."
        //                                     : "for any artifact" );
        this.runtimeOnly = runtimeOnly;
        this.selected = selected;
        this.filter =
            runtimeOnly ? new DependencyFilter( DependencyScope.runtime, ScopeTransitivity.maven, false, true, excludes )
                            : null;
        this.excludes.addAll( excludes );
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean result = false;

        if ( rel.isManaged() )
        {
            result = false;
        }
        else if ( rel.getType() == RelationshipType.PARENT )
        {
            result = true;
        }
        else
        {
            result = !excludes.contains( rel.getTarget()
                                            .asProjectRef() ) && ( filter == null || filter.accept( rel ) );

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

                // reset selections map to simulate classloader isolation.
                return new ShippingOrientedBuildFilter( true, excludes, new HashMap<ProjectRef, SingleVersion>() );
            }
            case PARENT:
            {
                return this;
            }
            default:
            {
                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;

                final Set<ProjectRef> exc = new HashSet<ProjectRef>();
                exc.addAll( excludes );
                if ( dr.getExcludes() != null )
                {
                    exc.addAll( dr.getExcludes() );
                }

                //                logger.info( "getChildFilter(%s)", lastRelationship );

                // As long as the scope is runtime or compile, this is still in the train to be rebuilt.
                // Otherwise, it's test or provided scope, and it's a build-requires situation...we don't need to rebuild it.
                return new ShippingOrientedBuildFilter( !DependencyScope.runtime.implies( dr.getScope() ), exc,
                                                        selected );
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

        sb.append( "; runtimeOnly=" )
          .append( runtimeOnly )
          .append( "; excludes=[" );
        for ( final Iterator<ProjectRef> iterator = excludes.iterator(); iterator.hasNext(); )
        {
            final ProjectRef exclude = iterator.next();
            sb.append( exclude );
            if ( iterator.hasNext() )
            {
                sb.append( ", " );
            }
        }
        sb.append( "])" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
