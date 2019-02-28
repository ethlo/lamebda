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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.security.UsernamePasswordCredentials;
import com.ethlo.lamebda.util.Assert;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProjectConfiguration implements Serializable
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectConfiguration.class);
    private final boolean listenForChanges;
    private final boolean isInfoProtected;

    private String rootContextPath;

    private String contextPath;

    private boolean enableInfoFunction;

    private boolean enableStaticResourceFunction;
    private String staticResourcesPrefix;
    private String staticResourceDirectory;

    private boolean enableUrlProjectContextPrefix;
    private String path;
    private String name;
    private transient UsernamePasswordCredentials adminCredentials;
    private String version;

    private String apiDocGenerator;
    private List<String> basePackages;

    ProjectConfiguration(ProjectConfigurationBuilder b)
    {
        rootContextPath = b.getRootContextPath();
        path = b.getProjectPath();
        staticResourcesPrefix = b.getStaticResourcesPrefix();
        contextPath = b.getProjectContextPath();
        staticResourceDirectory = b.getStaticResourceDirectory().toAbsolutePath().toString();
        enableInfoFunction = b.isEnableInfoFunction();
        isInfoProtected = b.isInfoProtected();
        enableStaticResourceFunction = b.isEnableStaticResourceFunction();
        enableUrlProjectContextPrefix = b.isEnableUrlProjectContextPrefix();
        name = b.getProjectName();
        version = b.getProjectVersion();
        adminCredentials = b.getAdminCredentials();
        apiDocGenerator = b.getApiDocGenerator();
        listenForChanges = b.isListenForChanges();
        basePackages = b.getBasePackages();
    }

    /**
     * @return The project level context mapping in the URL, for example <code>/gateway/<contextPath>/my-function</code>
     */
    @JsonProperty("mapping.project-context-path")
    public String getContextPath()
    {
        return contextPath;
    }

    /**
     * Enable info function to show simple function overview
     *
     * @return True if enabled, otherwise false
     */
    public boolean enableInfoFunction()
    {
        return enableInfoFunction;
    }

    @JsonProperty("function.static.enabled")
    public boolean enableStaticResourceFunction()
    {
        return enableStaticResourceFunction;
    }

    @JsonProperty("function.static.prefix")
    public String getStaticResourcesContext()
    {
        return staticResourcesPrefix;
    }

    @JsonProperty("function.static.path")
    public Path getStaticResourceDirectory()
    {
        return Paths.get(staticResourceDirectory).toAbsolutePath();
    }

    @JsonIgnore
    public Path getPath()
    {
        return Paths.get(path);
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

    @JsonProperty("functions.info.protected")
    public boolean isInfoProtected()
    {
        return isInfoProtected;
    }

    @JsonProperty("functions.info.enabled")
    public boolean isEnableInfoFunction()
    {
        return enableInfoFunction;
    }

    public boolean isEnableStaticResourceFunction()
    {
        return enableStaticResourceFunction;
    }

    public String getStaticResourcesPrefix()
    {
        return staticResourcesPrefix;
    }

    public boolean isEnableUrlProjectContextPrefix()
    {
        return enableUrlProjectContextPrefix;
    }

    /**
     * Returns the left-most URL path component, typically after the servlet context path. For example /servlet-name/<root-context-path>/my-function
     *
     * @return
     */
    public String getRootContextPath()
    {
        return rootContextPath;
    }

    @JsonIgnore
    public UsernamePasswordCredentials getAdminCredentials()
    {
        return this.adminCredentials;
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
                ", enableInfoFunction=" + enableInfoFunction +
                ", enableStaticResourceFunction=" + enableStaticResourceFunction +
                ", enableUrlProjectContextPrefix=" + enableUrlProjectContextPrefix +
                ", staticResourcesPrefix='" + staticResourcesPrefix + '\'' +
                ", staticResourceDirectory=" + staticResourceDirectory +
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
        return this.getPath().resolve(FileSystemLamebdaResourceLoader.LIB_DIRECTORY);
    }

    public Path getSpecificationPath()
    {
        return this.getPath().resolve("resources").resolve(FileSystemLamebdaResourceLoader.SPECIFICATION_DIRECTORY);
    }

    @JsonProperty("system.base-packages")
    public List<String> getBasePackages()
    {
        return this.basePackages;
    }

    public Path getTargetClassDirectory()
    {
        return getPath().resolve("target").resolve("classes");
    }

    public Path getGroovySourcePath()
    {
        return getPath().resolve("src").resolve("main").resolve("groovy");
    }
}
