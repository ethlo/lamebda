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

import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProjectConfiguration implements Serializable
{
    private final boolean listenForChanges;

    private final String rootContextPath;

    private final String contextPath;

    private final boolean enableUrlProjectContextPrefix;
    private final Path path;
    private final String name;
    private final String version;

    private final String apiDocGenerator;
    private final Set<String> basePackages;
    private final Set<Path> groovySourcePaths;
    private final Set<Path> javaSourcePaths;
    private final Set<URL> classPath;

    ProjectConfiguration(ProjectConfigurationBuilder b)
    {
        rootContextPath = b.getRootContextPath();
        path = b.getProjectPath();
        contextPath = b.getProjectContextPath();
        enableUrlProjectContextPrefix = b.isEnableUrlProjectContextPrefix();
        name = b.getProjectName();
        version = b.getProjectVersion();
        apiDocGenerator = b.getApiDocGenerator();
        listenForChanges = b.isListenForChanges();
        basePackages = b.getBasePackages();
        javaSourcePaths = b.getJavaSourcePaths();
        groovySourcePaths = b.getGroovySourcePaths();
        classPath = b.getClassPath();
    }

    /**
     * @return The project level context mapping in the URL, for example <code>/gateway/<contextPath>/my-function</code>
     */
    @JsonProperty("mapping.project-context-path")
    public String getContextPath()
    {
        return contextPath;
    }

    @JsonIgnore
    public Path getPath()
    {
        return path;
    }

    @JsonProperty("mapping.use-project-context-path")
    public boolean enableUrlProjectContextPrefix()
    {
        return enableUrlProjectContextPrefix;
    }

    public static ProjectConfigurationBuilder builder(String rootContextPath, Path projectPath)
    {
        return new ProjectConfigurationBuilder(rootContextPath, projectPath.toAbsolutePath());
    }

    @JsonProperty("project.name")
    public String getName()
    {
        return name;
    }

    @JsonProperty("project.version")
    public String getVersion()
    {
        return version;
    }

    @JsonProperty("specification.api.doc.generator")
    public String getApiDocGenerator()
    {
        return apiDocGenerator;
    }

    @JsonProperty("system.listen-for-changes")
    public boolean isListenForChanges()
    {
        return listenForChanges;
    }

    public boolean isEnableUrlProjectContextPrefix()
    {
        return enableUrlProjectContextPrefix;
    }

    /**
     * Returns the left-most URL path component, typically after the servlet context path. For example /servlet-name/&lt;root-context-path&gt;/my-function
     *
     * @return the left-most URL path component
     */
    public String getRootContextPath()
    {
        return rootContextPath;
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

    @Override
    public String toString()
    {
        return "ProjectConfigurationBuilder{" +
                "rootContextPath='" + rootContextPath + '\'' +
                ", path=" + path +
                ", name='" + name + '\'' +
                ", contextPath='" + contextPath + '\'' +
                ", enableUrlProjectContextPrefix=" + enableUrlProjectContextPrefix +
                '}';
    }

    public String toPrettyString()
    {
        try
        {
            return new ObjectMapper().writeValueAsString(this);
        }
        catch (JsonProcessingException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public Path getLibraryPath()
    {
        return this.getPath().resolve(FunctionManagerImpl.LIB_DIRECTORY);
    }

    public Path getSpecificationPath()
    {
        return this.getPath().resolve("src").resolve("main").resolve("resources").resolve(FunctionManagerImpl.SPECIFICATION_DIRECTORY);
    }

    @JsonProperty("system.base-packages")
    public Set<String> getBasePackages()
    {
        return this.basePackages;
    }

    public Path getTargetClassDirectory()
    {
        return getPath().resolve("target").resolve("classes");
    }

    public Set<Path> getGroovySourcePaths()
    {
        return groovySourcePaths;
    }

    public Path getMainResourcePath()
    {
        return getPath().resolve("src").resolve("main").resolve("resources");
    }

    public Set<Path> getJavaSourcePaths()
    {
        return javaSourcePaths;
    }

    public Set<URL> getClassPath()
    {
        return classPath;
    }

    public String staticUrlPath()
    {
        return getPath() + "/static/**";
    }
}
