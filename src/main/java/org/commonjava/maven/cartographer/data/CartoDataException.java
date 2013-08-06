/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.maven.cartographer.data;

import java.util.List;

import org.commonjava.maven.cartographer.CartoException;

public class CartoDataException
    extends CartoException
{

    private static final long serialVersionUID = 1L;

    private List<Throwable> nested;

    public CartoDataException( final String message, final Throwable error,
                                 final Object... params )
    {
        super( message, error, params );
    }

    public CartoDataException( final String message, final Object... params )
    {
        super( message, params );
    }

    public CartoDataException( final String message, final List<Throwable> nested,
                                 final Object... params )
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
                msg.append( "\n" ).append( idx ).append( ".  " ).append( error.getMessage() );
                idx++;
            }
        }

        return msg.toString();
    }
}
