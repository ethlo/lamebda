package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-core
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

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ProjectConfiguration
{
    private final Path path;
    private final String rootContextPath;
    private final ProjectInfo projectInfo;
    private final Set<URI> classpath = new LinkedHashSet<>();
    private String contextPath;

    public ProjectConfiguration(final BootstrapConfiguration bootstrapConfiguration, final Properties properties)
    {
        final boolean rootUrlPrefixEnabled = Boolean.parseBoolean(properties.getProperty("project.root-request-path-enabled", "true"));
        this.rootContextPath = rootUrlPrefixEnabled ? bootstrapConfiguration.getRootContextPath() : "";
        this.path = bootstrapConfiguration.getPath();

        final String id = bootstrapConfiguration.getPath().getFileName().toString();

        this.projectInfo = new ProjectInfo();
        this.projectInfo.setName(Optional.ofNullable(properties.getProperty("project.name")).orElse(id));
        this.projectInfo.setBasePackages(getCsvSet(properties));

        final boolean useProjectNameUrlPrefix = Boolean.parseBoolean(properties.getProperty("project.url-prefix-enabled", "true"));
        this.setContextPath(Optional.ofNullable(properties.getProperty("project.context-path")).orElse(useProjectNameUrlPrefix ? id : ""));
    }

    public static ProjectConfiguration load(final BootstrapConfiguration bootstrapConfiguration, final Path workDir)
    {
        final Path path = bootstrapConfiguration.getPath();
        final Path[] paths = Stream.of(path.resolve(ProjectImpl.PROJECT_FILENAME), workDir.resolve(ProjectImpl.PROJECT_FILENAME)).filter(Files::exists).toArray(Path[]::new);
        final Properties properties = merge(bootstrapConfiguration.getEnvProperties(), paths);
        return new ProjectConfiguration(bootstrapConfiguration, properties);
    }

    private static Properties merge(final Properties result, final Path... paths)
    {
        for (Path path : paths)
        {
            try (final Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
            {
                final Properties properties = new Properties();
                properties.load(reader);
                result.putAll(properties);
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }
        return result;
    }

    private Set<String> getCsvSet(final Properties properties)
    {
        return properties.getProperty("project.base-packages") != null ? StringUtils.commaDelimitedListToSet(properties.getProperty("project.base-packages")) : Collections.emptySet();
    }

    public String getRootContextPath()
    {
        return rootContextPath;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public void setContextPath(final String contextPath)
    {
        this.contextPath = contextPath;
    }

    public Path getPath()
    {
        return path;
    }

    public Set<URI> getClasspath()
    {
        return classpath;
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

    public ProjectInfo getProjectInfo()
    {
        return projectInfo;
    }
}
