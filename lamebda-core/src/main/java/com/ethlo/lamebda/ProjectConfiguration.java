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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.security.UsernamePasswordCredentials;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProjectConfiguration
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectConfiguration.class);

    private String rootContextPath;

    private String contextPath;

    private boolean enableInfoFunction;

    private boolean enableStaticResourceFunction;
    private String staticResourcesPrefix;
    private Path staticResourceDirectory;

    private boolean enableUrlProjectContextPrefix;
    private Path path;
    private String name;
    private UsernamePasswordCredentials adminCredentials;
    private String version;

    private ProjectConfiguration()
    {
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
        return staticResourceDirectory;
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
        return new ProjectConfigurationBuilder(rootContextPath, projectPath);
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

    public static final class ProjectConfigurationBuilder
    {
        private UsernamePasswordCredentials adminCredentials;
        private String rootContextPath;
        private Path projectPath;

        private String projectName;
        private String projectContextPath;
        private boolean enableInfoFunction;
        private boolean enableStaticResourceFunction;
        private boolean enableUrlProjectContextPrefix;

        private String staticResourcesPrefix;
        private Path staticResourceDirectory;
        private String projectVersion;

        private ProjectConfigurationBuilder(String rootContextPath, Path projectPath)
        {
            this.rootContextPath = rootContextPath;
            this.projectPath = projectPath;

            // Set defaults
            this.projectName = projectPath.getFileName().toString();
            this.projectContextPath = projectPath.getFileName().toString();
            this.enableInfoFunction = true;
            this.enableStaticResourceFunction = true;
            this.enableUrlProjectContextPrefix = true;
            this.staticResourcesPrefix = "static";
            this.projectPath.resolve(FileSystemLamebdaResourceLoader.STATIC_DIRECTORY);
            this.staticResourceDirectory = projectPath.resolve(FileSystemLamebdaResourceLoader.STATIC_DIRECTORY);
            this.adminCredentials = new UsernamePasswordCredentials("admin", UUID.randomUUID().toString());
        }

        public ProjectConfigurationBuilder projectContextPath(String projectContextPath)
        {
            this.projectContextPath = projectContextPath;
            return this;
        }

        public ProjectConfigurationBuilder enableInfoFunction(boolean enableInfoFunction)
        {
            this.enableInfoFunction = enableInfoFunction;
            return this;
        }

        public ProjectConfigurationBuilder enableStaticResourceFunction(boolean enableStaticResourceFunction)
        {
            this.enableStaticResourceFunction = enableStaticResourceFunction;
            return this;
        }

        public ProjectConfigurationBuilder enableUrlProjectContextPrefix(boolean enableUrlProjectContextPrefix)
        {
            this.enableUrlProjectContextPrefix = enableUrlProjectContextPrefix;
            return this;
        }

        public ProjectConfigurationBuilder staticResourcesPrefix(String staticResourcesPrefix)
        {
            this.staticResourcesPrefix = staticResourcesPrefix;
            return this;
        }

        public ProjectConfigurationBuilder staticResourceDirectory(Path staticResourceDirectory)
        {
            this.staticResourceDirectory = staticResourceDirectory;
            return this;
        }

        public ProjectConfigurationBuilder projectName(String projectName)
        {
            this.projectName = projectName;
            return this;
        }

        public ProjectConfiguration build()
        {
            ProjectConfiguration projectConfiguration = new ProjectConfiguration();
            projectConfiguration.rootContextPath = this.rootContextPath;
            projectConfiguration.path = this.projectPath;
            projectConfiguration.staticResourcesPrefix = this.staticResourcesPrefix;
            projectConfiguration.contextPath = this.projectContextPath;
            projectConfiguration.staticResourceDirectory = this.staticResourceDirectory;
            projectConfiguration.enableInfoFunction = this.enableInfoFunction;
            projectConfiguration.enableStaticResourceFunction = this.enableStaticResourceFunction;
            projectConfiguration.enableUrlProjectContextPrefix = this.enableUrlProjectContextPrefix;
            projectConfiguration.name = this.projectName;
            projectConfiguration.version = this.projectVersion;
            projectConfiguration.adminCredentials = this.adminCredentials;
            return projectConfiguration;
        }

        public ProjectConfigurationBuilder loadIfExists()
        {
            final Path projectConfigFile = projectPath.resolve(FileSystemLamebdaResourceLoader.PROJECT_FILENAME);
            if (Files.exists(projectConfigFile))
            {
                final Properties p = new Properties();
                try (InputStream in = Files.newInputStream(projectConfigFile))
                {
                    p.load(in);
                }
                catch (IOException exc)
                {
                    throw new UncheckedIOException(exc);
                }

                projectName = p.getProperty("project.name", projectName);
                projectVersion = p.getProperty("project.version");

                // URL mapping
                projectContextPath = p.getProperty("mapping.project-context-path", projectContextPath);
                enableUrlProjectContextPrefix = Boolean.parseBoolean(p.getProperty("mapping.use-project-context-path", Boolean.toString(enableUrlProjectContextPrefix)));

                // Static resource function
                enableStaticResourceFunction = Boolean.parseBoolean(p.getProperty("functions.static.enabled", Boolean.toString(enableStaticResourceFunction)));
                staticResourcesPrefix = p.getProperty("functions.static.prefix", staticResourcesPrefix);
                staticResourceDirectory = Paths.get(p.getProperty("function.static.path", staticResourcesPrefix.toString()));

                final String adminUsername = p.getProperty("admin.credentials.username", "admin");
                String adminPassword = p.getProperty("admin.credentials.password");
                if (adminPassword == null)
                {
                    adminPassword = RandomStringUtils.randomAlphanumeric(12);
                    logger.info("Using generated admin password: {}. Please set it using 'admin.credentials.password=' in project.properties", adminPassword);
                }
                adminCredentials = new UsernamePasswordCredentials(adminUsername, adminPassword);

                // Info function
                enableInfoFunction = Boolean.parseBoolean(p.getProperty("functions.info.enabled", Boolean.toString(enableInfoFunction)));
            }
            return this;
        }
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
        return "" +
                "name='" + name + '\'' +
                "\ncontextPath='" + contextPath + '\'' +
                "\nrootContextPath='" + rootContextPath + '\'' +
                "\npath=" + path +
                "\nenableInfoFunction=" + enableInfoFunction +
                "\nenableStaticResourceFunction=" + enableStaticResourceFunction +
                "\nenableUrlProjectContextPrefix=" + enableUrlProjectContextPrefix +
                "\nstaticResourcesPrefix='" + staticResourcesPrefix + '\'' +
                "\nstaticResourceDirectory=" + staticResourceDirectory
                + "";
    }
}
