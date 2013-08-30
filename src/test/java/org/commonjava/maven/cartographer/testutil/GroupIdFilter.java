package org.commonjava.maven.cartographer.testutil;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class GroupIdFilter
    implements ProjectRelationshipFilter
{

    private final String groupId;

    public GroupIdFilter( final String groupId )
    {
        this.groupId = groupId;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        return groupId.equals( rel.getTarget()
                                  .getGroupId() );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return new GroupIdFilter( groupId + ".child" );
    }

    @Override
    public void render( final StringBuilder sb )
    {
        sb.append( "Artifacts with groupId [" )
          .append( groupId )
          .append( ']' );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }
}