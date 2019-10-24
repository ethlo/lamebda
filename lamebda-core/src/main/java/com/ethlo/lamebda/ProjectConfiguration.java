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
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

import com.ethlo.lamebda.util.IoUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Valid
public class ProjectConfiguration implements Serializable
{
    private final Path path;
    private final String rootContextPath;

    private boolean enableUrlProjectContextPrefix = true;

    private String contextPath;

    @NotNull
    private ProjectInfo project;

    private String apiDocGenerator;

    private Set<Path> groovySourcePaths = new LinkedHashSet<>();
    private Set<Path> javaSourcePaths = new LinkedHashSet<>();

    private Set<URL> classpath = new LinkedHashSet<>();

    private DeploymentConfig deploymentConfig;

    public ProjectConfiguration(final Path path, final String rootContextPath)
    {
        this.path = path.toAbsolutePath();
        this.project = new ProjectInfo();
        this.project.setName(path.getFileName().toString());
        this.rootContextPath = rootContextPath;
    }

    public static ProjectConfiguration load(@NotNull final String rootContext, @NotNull final Path projectConfigFile)
    {
        final Properties properties = new Properties();
        if (Files.exists(projectConfigFile))
        {
            try (InputStream in = Files.newInputStream(projectConfigFile))
            {
                properties.load(in);
                return load(rootContext, projectConfigFile.getParent(), properties);
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }
        return load(rootContext, projectConfigFile.getParent(), properties);
    }

    public static ProjectConfiguration load(@NotNull final String rootContextPath, @NotNull final Path projectPath, @NotNull final Properties properties)
    {
        final ProjectConfiguration cfg = new ProjectConfiguration(projectPath, rootContextPath);

        final Optional<String> optVersion = IoUtil.toString(projectPath.resolve("version"));
        optVersion.ifPresent(versionStr->
        {
            cfg.getProject().setVersion(versionStr);
        });

        ConfigurationUtil.populate(cfg, properties);
        if (cfg.getContextPath() == null && cfg.enableUrlProjectContextPrefix)
        {
            cfg.setContextPath(projectPath.getFileName().toString());
        }
        return cfg;
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

    public ProjectConfiguration setProject(final ProjectInfo project)
    {
        this.project = project;
        return this;
    }
}
