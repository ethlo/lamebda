package com.ethlo.lamebda.spring;

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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Valid
@Validated
@Component
@ConfigurationProperties(prefix = "lamebda")
public class LamebdaRootConfiguration
{
    /**
     * The root request path in the URL
     */
    private String requestPath = "/lamebda";

    /**
     * Turn Lamebda on/off
     */
    private boolean enabled;

    /**
     * The directory to load/store projects from. All projects will be in a sub-directory of this folder with the name of the project.
     */
    @NotNull
    private Path rootDirectory;

    public String getRequestPath()
    {
        return requestPath;
    }

    public LamebdaRootConfiguration setRequestPath(final String requestPath)
    {
        this.requestPath = requestPath;
        return this;
    }

    public Path getRootDirectory()
    {
        return rootDirectory;
    }

    public LamebdaRootConfiguration setRootDirectory(final Path rootDirectory)
    {
        this.rootDirectory = rootDirectory;
        return this;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public LamebdaRootConfiguration setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }
}
