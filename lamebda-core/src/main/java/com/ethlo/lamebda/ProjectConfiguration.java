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
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ProjectConfiguration
{
    private final Path path;
    private final String rootContextPath;
    private final Path workDir;
    @NotNull
    private final ProjectInfo project;
    private boolean enableUrlProjectContextPrefix = true;
    private String contextPath;
    private String apiDocGenerator;

    private Set<Path> groovySourcePaths = new LinkedHashSet<>();
    private Set<Path> javaSourcePaths = new LinkedHashSet<>();

    private Set<URL> classpath = new LinkedHashSet<>();

    private DeploymentConfig deploymentConfig;

    public ProjectConfiguration(final Path workDir, final BootstrapConfiguration bootstrapConfiguration, Properties properties)
    {
        this.rootContextPath = bootstrapConfiguration.getRootContextPath();
        this.path = bootstrapConfiguration.getPath();
        this.workDir = workDir;

        final String id = bootstrapConfiguration.getPath().getFileName().toString();

        this.project = new ProjectInfo();
        this.project.setName(properties.getProperty("project.name") != null ? properties.getProperty("project.name") : id);
        this.project.setBasePackages(properties.getProperty("project.base-packages") != null ? StringUtils.commaDelimitedListToSet(properties.getProperty("project.base-packages")) : Collections.emptySet());
        this.setContextPath(properties.getProperty("project.context-path") != null ? properties.getProperty("project.context-path") : id);
    }

    public static ProjectConfiguration load(final BootstrapConfiguration bootstrapConfiguration, final Path workDir)
    {
        final Path path = bootstrapConfiguration.getPath();
        final Path[] paths = Stream.of(path.resolve(ProjectImpl.PROJECT_FILENAME), workDir.resolve(ProjectImpl.PROJECT_FILENAME)).filter(p -> Files.exists(p)).toArray(Path[]::new);
        final Properties properties = merge(bootstrapConfiguration.getEnvProperties(), paths);
        return new ProjectConfiguration(workDir, bootstrapConfiguration, properties);
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

    public ProjectConfiguration setContextPath(final String contextPath)
    {
        this.contextPath = contextPath;
        return this;
    }

    public boolean enableUrlProjectContextPrefix()
    {
        return enableUrlProjectContextPrefix;
    }

    public ProjectConfiguration setEnableUrlProjectContextPrefix(final boolean enableUrlProjectContextPrefix)
    {
        this.enableUrlProjectContextPrefix = enableUrlProjectContextPrefix;
        return this;
    }

    public Path getPath()
    {
        return path;
    }

    public String getApiDocGenerator()
    {
        return apiDocGenerator;
    }

    public ProjectConfiguration setApiDocGenerator(final String apiDocGenerator)
    {
        this.apiDocGenerator = apiDocGenerator;
        return this;
    }

    public Set<Path> getGroovySourcePaths()
    {
        return merge(getPath().resolve("src").resolve("main").resolve("groovy"), groovySourcePaths);
    }

    public ProjectConfiguration setGroovySourcePaths(final Set<Path> groovySourcePaths)
    {
        this.groovySourcePaths = ensureAbsolutePaths(groovySourcePaths);
        return this;
    }

    public Set<Path> getJavaSourcePaths()
    {
        return merge(getPath().resolve("src").resolve("main").resolve("java"), javaSourcePaths);
    }

    public ProjectConfiguration setJavaSourcePaths(final Set<Path> javaSourcePaths)
    {
        this.javaSourcePaths = ensureAbsolutePaths(javaSourcePaths);
        return this;
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

    public ProjectConfiguration setClasspath(final Set<URL> classPath)
    {
        this.classpath = classPath;
        return this;
    }

    /**
     * Use setClasspath instead
     */
    @Deprecated
    public ProjectConfiguration setClassPath(final Set<URL> classPath)
    {
        this.classpath = classPath;
        return this;
    }

    public String getJavaCmd()
    {
        final String osName = System.getProperty("os.name").toLowerCase();
        final boolean isWindows = osName.contains("win");
        final String javaHome = System.getProperty("java.home");
        Assert.isTrue(javaHome != null, "java.home system property must be set");
        final String execPath = isWindows ? "bin/java.exe" : "bin/java";
        return Paths.get(javaHome).resolve(execPath).toAbsolutePath().toString();
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

    public Path getLibraryPath()
    {
        return path.resolve(ProjectImpl.LIB_DIRECTORY);
    }

    public Path getSpecificationPath()
    {
        return this.getPath().resolve("src").resolve("main").resolve("resources").resolve(ProjectImpl.SPECIFICATION_DIRECTORY);
    }

    public Path getTargetClassDirectory()
    {
        return path.resolve("target").resolve("classes");
    }

    public Path getMainResourcePath()
    {
        return path.resolve("src").resolve("main").resolve("resources");
    }

    public DeploymentConfig getDeploymentConfig()
    {
        return deploymentConfig;
    }

    public ProjectConfiguration setDeploymentConfig(final DeploymentConfig deploymentConfig)
    {
        this.deploymentConfig = deploymentConfig;
        return this;
    }

    public ProjectInfo getProject()
    {
        return project;
    }

    public Path getWorkDirectory()
    {
        return workDir;
    }

    public void addJavaSourcePath(final Path path)
    {
        javaSourcePaths.add(path);
    }
}
