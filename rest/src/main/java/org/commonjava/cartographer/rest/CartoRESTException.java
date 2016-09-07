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
