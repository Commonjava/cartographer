/**
 * Copyright (C) 2015 John Casey (jdcasey@commonjava.org)
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

import javax.enterprise.context.ApplicationScoped;
import java.io.File;

@SectionName("swagger")
@ApplicationScoped
public class CartoSwaggerConfig
        implements CartoSubConfig
{

    private static final boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_RESOURCE_PACKAGE = "org.commonjava.cartographer.rest";
    private static final String DEFAULT_BASE_PATH = "/";
    private static final String DEFAULT_LICENSE = "ASLv2";
    private static final String DEFAULT_LICENSE_URL = "http://www.apache.org/licenses/LICENSE-2.0";
    private static final String DEFAULT_VERSION = "0.1";

    private Boolean enabled;
    private String basePath;
    private String license;
    private String licenseUrl;
    private String version;
    private String resourcePackage;

    public CartoSwaggerConfig() {}

    public String getBasePath() {
        return basePath == null ? DEFAULT_BASE_PATH : basePath;
    }

    @ConfigName( "base.path")
    public void setBasePath(final String basePath) {
        this.basePath = basePath;
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public String getLicense() {
        return license == null? DEFAULT_LICENSE : license;
    }

    @ConfigName( "license")
    public void setLicense(final String license) {
        this.license = license;
    }

    public String getLicenseUrl() {
        return licenseUrl == null ? DEFAULT_LICENSE_URL : licenseUrl;
    }

    @ConfigName( "license.url")
    public void setLicenseUrl(final String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public String getVersion() { return version == null ? DEFAULT_VERSION : version; }

    @ConfigName( "version")
    public void setVersion(final String version) {
        this.version = version;
    }

    public String getResourcePackage() {
        return resourcePackage == null ? DEFAULT_RESOURCE_PACKAGE : resourcePackage;
    }

    @ConfigName( "resource.package")
    public void setResourcePackage(final String resourcePackage) {
        this.resourcePackage = resourcePackage;
    }

}
