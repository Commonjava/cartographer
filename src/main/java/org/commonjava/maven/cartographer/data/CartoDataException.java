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
package org.commonjava.maven.cartographer.data;

import java.util.List;

import org.commonjava.maven.cartographer.CartoException;

public class CartoDataException
    extends CartoException
{

    private static final long serialVersionUID = 1L;

    private List<Throwable> nested;

    public CartoDataException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

    public CartoDataException( final String message, final Object... params )
    {
        super( message, params );
    }

    public CartoDataException( final String message, final List<Throwable> nested, final Object... params )
    {
        super( message, params );
        this.nested = nested;
    }

    @Override
    public String getMessage()
    {
        StringBuilder msg = new StringBuilder( super.getMessage() );
        if ( nested != null && !nested.isEmpty() )
        {
            msg.append( "\nNested errors:\n" );

            int idx = 1;
            for ( Throwable error : nested )
            {
                msg.append( "\n" )
                   .append( idx )
                   .append( ".  " )
                   .append( error.getMessage() );
                idx++;
            }
        }

        return msg.toString();
    }
}
