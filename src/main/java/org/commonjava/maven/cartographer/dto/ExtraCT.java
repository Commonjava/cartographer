/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.dto;

import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;

public class ExtraCT
{

    public static final String WILDCARD = "*";

    private String classifier;

    private String type;

    public ExtraCT()
    {
    }

    public ExtraCT( final String type )
    {
        this.type = type;
    }

    public ExtraCT( final String type, final String classifier )
    {
        this.type = type;
        this.classifier = classifier;
    }

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
