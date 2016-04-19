package org.commonjava.cartographer.rest;

import org.commonjava.cartographer.CartoException;

import static org.commonjava.propulsor.deploy.undertow.util.ApplicationStatus.BAD_REQUEST;

/**
 * Created by jdcasey on 4/18/16.
 */
public class CartoRESTException
        extends CartoException
{
    private int code;

    public CartoRESTException( int code, String message, Throwable error, Object... params )
    {
        super( message, error, params );
        this.code = code;
    }

    public CartoRESTException( int code, String message, Object... params )
    {
        super( message, params );
        this.code = code;
    }

    public CartoRESTException( String message, Throwable error, Object... params )
    {
        super( message, error, params );
        this.code = BAD_REQUEST.code();
    }

    public CartoRESTException( String message, Object... params )
    {
        super( message, params );
        this.code = BAD_REQUEST.code();
    }

    public int getCode()
    {
        return code;
    }
}
