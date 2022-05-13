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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
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
    private String contextPath;

    private Set<Path> groovySourcePaths = new LinkedHashSet<>();
    private Set<Path> javaSourcePaths = new LinkedHashSet<>();

    private final Set<URL> classpath = new LinkedHashSet<>();
    private final String apiSpecificationSource;

    public ProjectConfiguration(final BootstrapConfiguration bootstrapConfiguration, Properties properties)
    {
        final boolean rootUrlPrefixEnabled = Boolean.parseBoolean(properties.getProperty("project.root-request-path-enabled", "true"));
        this.rootContextPath = rootUrlPrefixEnabled ? bootstrapConfiguration.getRootContextPath() : "";
        this.path = bootstrapConfiguration.getPath();

        final String id = bootstrapConfiguration.getPath().getFileName().toString();

        this.projectInfo = new ProjectInfo();
        this.projectInfo.setName(Optional.ofNullable(properties.getProperty("project.name")).orElse(id));
        this.projectInfo.setBasePackages(getCsvSet("project.base-packages", properties));

        this.apiSpecificationSource = Optional.ofNullable(properties.getProperty("project.api-specification.source")).orElse("specification/oas.yaml");

        final boolean useProjectNameUrlPrefix = Boolean.parseBoolean(properties.getProperty("project.url-prefix-enabled", "true"));
        this.setContextPath(Optional.ofNullable(properties.getProperty("project.context-path")).orElse(useProjectNameUrlPrefix ? id : ""));

        this.setJavaSourcePaths(merge(getPath().resolve("src").resolve("main").resolve("java"), getCsvSet("project.java.sources", properties).stream().map(Paths::get).collect(Collectors.toSet())));
        this.setGroovySourcePaths(merge(getPath().resolve("src").resolve("main").resolve("groovy"), getCsvSet("project.groovy.sources", properties).stream().map(Paths::get).collect(Collectors.toSet())));
    }

    private Set<String> getCsvSet(final String setting, final Properties properties)
    {
        return properties.getProperty(setting) != null ? StringUtils.commaDelimitedListToSet(properties.getProperty(setting)) : Collections.emptySet();
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

    public Set<Path> getGroovySourcePaths()
    {
        return groovySourcePaths;
    }

    public void setGroovySourcePaths(final Set<Path> groovySourcePaths)
    {
        this.groovySourcePaths = ensureAbsolutePaths(groovySourcePaths);
    }

    public Set<Path> getJavaSourcePaths()
    {
        return javaSourcePaths;
    }

    public void setJavaSourcePaths(final Set<Path> javaSourcePaths)
    {
        this.javaSourcePaths = ensureAbsolutePaths(javaSourcePaths);
    }

    private Set<Path> merge(final Path extra, final Set<Path> existing)
    {
        existing.add(extra);
        return existing;
    }

    private Set<Path> ensureAbsolutePaths(final Set<Path> paths)
    {
        return paths.stream().map(p -> p.isAbsolute() ? p : path.resolve(p).normalize()).collect(Collectors.toSet());
    }

    public Set<URL> getClasspath()
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

    public Path getTargetClassDirectory()
    {
        return path.resolve("target").resolve("classes");
    }

    public Path getMainResourcePath()
    {
        return path.resolve("src").resolve("main").resolve("resources");
    }

    public ProjectInfo getProjectInfo()
    {
        return projectInfo;
    }

    public String getApiSpecificationSource()
    {
        return apiSpecificationSource;
    }
}
