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
package org.commonjava.maven.cartographer.discover.post.meta;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.galley.maven.parse.MavenPomReader;

@ApplicationScoped
public class ScmUrlScanner
    extends AbstractMetadataScanner
    implements MetadataScanner
{

    protected ScmUrlScanner()
    {
    }

    public ScmUrlScanner( final MavenPomReader pomReader )
    {
        super( pomReader );
    }

    private static final Map<String, String> KEYS_TO_PATHS = new HashMap<String, String>()
    {
        {
            put( "scm-connection", "/project/scm/connection/text()" );
            put( "scm-url", "/project/scm/url/text()" );
        }

        private static final long serialVersionUID = 1L;
    };

    @Override
    protected Map<String, String> getMetadataKeyXPathMappings()
    {
        return KEYS_TO_PATHS;
    }
}
