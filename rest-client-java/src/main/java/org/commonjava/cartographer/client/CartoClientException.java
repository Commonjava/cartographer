package org.commonjava.cartographer.client;

import org.commonjava.cartographer.CartoException;

/**
 * Created by jdcasey on 4/18/16.
 */
public class CartoClientException
        extends CartoException
{
    public CartoClientException( String message, Throwable error, Object... params )
    {
        super( message, error, params );
    }

    public CartoClientException( String message, Object... params )
    {
        super( message, params );
    }

}
