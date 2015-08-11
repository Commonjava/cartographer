/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.cartographer.request;

import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;

public class ExtraCT
    implements Comparable<ExtraCT>
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

    @Override
    public int compareTo( final ExtraCT other )
    {
        int comp = getType().compareTo( other.getType() );
        if ( comp == 0 )
        {
            if ( classifier == null && other.classifier != null )
            {
                return -1;
            }
            else if ( classifier != null && other.classifier == null )
            {
                return 1;
            }
            else
            {
                comp = classifier.compareTo( other.classifier );
            }
        }

        return comp;
    }

}
