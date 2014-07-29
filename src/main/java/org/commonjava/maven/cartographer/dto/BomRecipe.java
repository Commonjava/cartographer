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
package org.commonjava.maven.cartographer.dto;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class BomRecipe
    extends RepositoryContentRecipe
{

    private ProjectVersionRef output;

    private boolean generateVersionRanges;

    public BomRecipe()
    {
    }

    public boolean isGenerateVersionRanges()
    {
        return generateVersionRanges;
    }

    public void setGenerateVersionRanges( final boolean generateVersionRanges )
    {
        this.generateVersionRanges = generateVersionRanges;
    }

    public ProjectVersionRef getOutput()
    {
        return output;
    }

    public void setOutput( final ProjectVersionRef outputGav )
    {
        this.output = outputGav;
    }

}
