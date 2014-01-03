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
package org.commonjava.maven.cartographer.dto;

import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;

public class ExtraCT
{

    public static final String WILDCARD = "*";

    private String classifier;

    private String type;

    public String getClassifier()
    {
        return classifier;
    }

    public String getType()
    {
        return type == null ? "jar" : type;
    }

    public void setClassifier( final String classifier )
    {
        this.classifier = classifier;
    }

    public void setType( final String type )
    {
        this.type = type;
    }

    public boolean matches( final TypeAndClassifier tc )
    {
        final String t = getType();
        final String c = getClassifier();

        final boolean typeMatch = WILDCARD.equals( t ) || t.equals( tc.getType() );
        final boolean clsMatch = WILDCARD.equals( c ) || c.equals( tc.getClassifier() );

        return typeMatch && clsMatch;
    }

}
