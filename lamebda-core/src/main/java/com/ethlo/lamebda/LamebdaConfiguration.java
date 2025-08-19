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

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
     * Turn Lamebda on/off
     */
    private final boolean enabled;

    /**
     * The directory to load/store projects from. All projects will be in a subdirectory of this folder with the name of the project.
     */
    @NotNull
    private final Path rootDirectory;

    /**
     * If enabled the projects will reload automatically if there are any file modifications in the project
     */
    private final boolean directoryWatchEnabled;
    /**
     * Whether the failure of loading a project should halt or log a warning
     */
    private final boolean haltOnError;

    private final Set<String> requiredProjects;

    public LamebdaConfiguration(final String requestPath, final Boolean enabled, final Path rootDirectory, final Boolean directoryWatchEnabled, final Boolean haltOnError, Set<String> requiredProjects)
    {
        this.requestPath = requestPath;
        this.enabled = Optional.ofNullable(enabled).orElse(true);
        this.rootDirectory = Optional.ofNullable(rootDirectory).map(dir -> Paths.get(dir.toString().replaceFirst("^~", System.getProperty("user.home")))).orElseThrow(() -> new IllegalArgumentException("The Lamebda root directory must be set"));
        this.directoryWatchEnabled = Optional.ofNullable(directoryWatchEnabled).orElse(true);
        this.haltOnError = Optional.ofNullable(haltOnError).orElse(true);
        this.requiredProjects = requiredProjects;
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

    public boolean isDirectoryWatchEnabled()
    {
        return directoryWatchEnabled;
    }

    public Set<String> getRequiredProjects()
    {
        return Optional.ofNullable(requiredProjects).orElse(Set.of());
    }

    public String toPrettyString()
    {
        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public boolean haltOnError()
    {
        return haltOnError;
    }
}
