package org.commonjava.maven.cartographer.util.maven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MavenPomView
{

    private final ProjectVersionRef ref;

    private final Document pomDoc;

    private final Document[] ancestorDocs;

    private final XPath xpath;

    private StringSearchInterpolator ssi;

    private final Map<String, XPathExpression> xpaths = new HashMap<>();

    public MavenPomView( final ProjectVersionRef ref, final Document pomDoc, final Document... ancestorDocs )
    {
        this.ref = ref;
        this.pomDoc = pomDoc;
        this.ancestorDocs = ancestorDocs;
        this.xpath = XPathFactory.newInstance()
                                 .newXPath();
    }

    public ProjectVersionRef getGAV()
    {
        return ref;
    }

    public Document[] getAncestorDocuments()
    {
        return ancestorDocs;
    }

    public Document getPomDocument()
    {
        return pomDoc;
    }

    public String resolveMavenExpression( final String expression )
        throws MavenPomException
    {
        return resolveXPathExpression( expression.replace( '.', '/' ), false );
    }

    public String resolveXPathExpression( String path, final boolean localOnly )
        throws MavenPomException
    {
        if ( !path.endsWith( "/text()" ) )
        {
            path += "/text()";
        }

        final Node result = resolveXPathToNode( path, localOnly );
        if ( result != null && result.getNodeType() == Node.TEXT_NODE )
        {
            return resolveExpressions( result.getTextContent()
                                             .trim() );
        }

        return null;
    }

    public Element resolveXPathToElement( final String path, final boolean localOnly )
        throws MavenPomException
    {
        final Node node = resolveXPathToNode( path, localOnly );
        if ( node != null && node.getNodeType() == Node.ELEMENT_NODE )
        {
            return (Element) node;
        }

        return null;
    }

    public synchronized Node resolveXPathToNode( final String path, final boolean localOnly )
        throws MavenPomException
    {
        try
        {
            XPathExpression expression = xpaths.get( path );
            if ( expression == null )
            {
                expression = xpath.compile( path );
                xpaths.put( path, expression );
            }

            Node result = (Node) expression.evaluate( pomDoc, XPathConstants.NODE );
            if ( !localOnly )
            {
                for ( final Document ancestor : ancestorDocs )
                {
                    if ( result != null )
                    {
                        break;
                    }

                    result = (Node) expression.evaluate( ancestor, XPathConstants.NODE );
                }
            }

            return result;
        }
        catch ( final XPathExpressionException e )
        {
            throw new MavenPomException( "Failed to retrieve content for xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }
    }

    private String resolveExpressions( final String value )
        throws MavenPomException
    {
        synchronized ( this )
        {
            if ( ssi == null )
            {
                ssi = new StringSearchInterpolator();
                ssi.addValueSource( new MavenPomViewVS( this ) );
            }
        }

        try
        {
            return ssi.interpolate( value );
        }
        catch ( final InterpolationException e )
        {
            throw new MavenPomException( "Failed to interpolate expressions in: '%s'. Reason: %s", e, value, e.getMessage() );
        }
    }

    public class MavenPomViewVS
        implements ValueSource
    {

        private final MavenPomView view;

        private final List<Object> feedback = new ArrayList<>();

        public MavenPomViewVS( final MavenPomView view )
        {
            this.view = view;
        }

        @Override
        public void clearFeedback()
        {
            feedback.clear();
        }

        @SuppressWarnings( "rawtypes" )
        @Override
        public List getFeedback()
        {
            return feedback;
        }

        @Override
        public Object getValue( final String expr )
        {
            try
            {
                return view.resolveMavenExpression( expr );
            }
            catch ( final MavenPomException e )
            {
                feedback.add( String.format( "Error resolving maven expression: '%s'", expr ) );
                feedback.add( e );
            }

            return expr;
        }

    }

}
