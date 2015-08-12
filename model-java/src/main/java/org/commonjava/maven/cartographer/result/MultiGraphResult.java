package org.commonjava.maven.cartographer.result;

import org.commonjava.maven.cartographer.result.GraphResult;

import java.util.ArrayList;
import java.util.List;

public class MultiGraphResult<T extends GraphResult>
{
    private List<T> results = new ArrayList<>();

    public MultiGraphResult()
    {
    }

    public MultiGraphResult( final List<T> results )
    {
        if ( results != null )
        {
            this.results.addAll( results );
        }
    }

    public List<T> getResults()
    {
        return results;
    }

    public void setResults( final List<T> results )
    {
        this.results = results;
    }

    public void add( final T result )
    {
        results.add( result );
    }
}
