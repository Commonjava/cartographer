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
package org.commonjava.maven.cartographer.ftest.testutil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 9/9/16.
 */
public final class TestFileUtils
{
    private TestFileUtils(){}

    public static String readResource( String name )
    {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( name ))
        {
            if ( in == null )
            {
                fail( "Cannot find '" + name + "' on test classpath!" );
            }

            return IOUtils.toString( in );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail( "Failed to read from test classpath: " + name + ". Reason: " + e.getMessage() );
        }

        return null;
    }

    public static void writeConfigFile( File dir, String fname, String content )
    {
        File f = new File( dir, fname );
        try
        {
            FileUtils.write( f, content );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail( "Failed to write: " + f + ". Reason: " + e.getMessage() );
        }
    }

}
