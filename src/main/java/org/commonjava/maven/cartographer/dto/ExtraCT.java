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
