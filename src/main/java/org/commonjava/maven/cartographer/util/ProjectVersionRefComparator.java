/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.cartographer.util;

import java.util.Comparator;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

public class ProjectVersionRefComparator
    implements Comparator<ProjectVersionRef>
{

    @Override
    public int compare( final ProjectVersionRef first, final ProjectVersionRef second )
    {
        int comp = first.getGroupId()
                        .compareTo( second.getGroupId() );

        if ( comp == 0 )
        {
            comp = first.getArtifactId()
                        .compareTo( second.getArtifactId() );
        }

        if ( comp == 0 )
        {
            try
            {
                comp = first.getVersionSpec()
                            .compareTo( second.getVersionSpec() );
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                comp = first.getVersionString()
                            .compareTo( second.getVersionString() );
            }
        }

        return comp;
    }

}
