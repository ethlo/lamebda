package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-spring-web-starter
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Valid
@Validated
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "lamebda")
public class LamebdaConfiguration
{
    /**
     * The root request path in the URL
     */
    private final String requestPath;

    /**
     * The root path for the UI rendering of the Lamebda index page. Usually not required to be modified.
     */
    private final String uiBasePath;

    /**
     * If you want to render your OpenAPI APIs via Swagger UI, the class-path to the swagger UI base-path must be defined
     */
    private final String swaggerUiPath;

    /**
     * Turn Lamebda on/off
     */
    private final boolean enabled;

    /**
     * The directory to load/store projects from. All projects will be in a subdirectory of this folder with the name of the project.
     */
    @NotNull
    private final Path rootDirectory;

    private final Boolean directoryWatchEnabled;
    private final Boolean haltOnError;

    public LamebdaConfiguration(final String requestPath, final String uiBasePath, final String swaggerUiPath, final boolean enabled, final Path rootDirectory, final Boolean directoryWatchEnabled, final Boolean haltOnError)
    {
        this.requestPath = requestPath;
        this.uiBasePath = uiBasePath;
        this.swaggerUiPath = swaggerUiPath;
        this.enabled = enabled;
        this.rootDirectory = Paths.get(rootDirectory.toString().replaceFirst("^~", System.getProperty("user.home")));
        this.directoryWatchEnabled = directoryWatchEnabled;
        this.haltOnError = haltOnError;
    }

    public String getRequestPath()
    {
        return requestPath;
    }

    public Path getRootDirectory()
    {
        return rootDirectory;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public String getSwaggerUiPath()
    {
        return swaggerUiPath;
    }

    public String getUiBasePath()
    {
        return uiBasePath;
    }

    public boolean isDirectoryWatchEnabled()
    {
        return directoryWatchEnabled != null ? directoryWatchEnabled : true;
    }

    @Override
    public String toString()
    {
        return
                "request-path: " + requestPath + '\n' +
                        "ui-base-path: " + uiBasePath + '\n' +
                        "swagger-ui-path: " + swaggerUiPath + '\n' +
                        "root-directory: " + rootDirectory;
    }

    public boolean haltOnError()
    {
        return haltOnError != null ? haltOnError : false;
    }
}
