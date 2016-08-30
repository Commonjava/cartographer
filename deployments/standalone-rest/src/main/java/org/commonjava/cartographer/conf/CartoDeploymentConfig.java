/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.conf;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.join;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@Alternative
@Named
public class CartoDeploymentConfig
    extends CartographerConfig
{
    public static final String INDY_URL_KEY = "indy.url";

    private String indyUrl;

    public String getIndyUrl()
    {
        return indyUrl;
    }

    @ConfigName( CartoDeploymentConfig.INDY_URL_KEY )
    public void setIndyUrl (String indyUrl )
    {
        this.indyUrl = indyUrl;
    }

}
