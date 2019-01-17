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
import java.util.Properties;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;

public class ProjectConfiguration
{
    private String rootContextPath;

    private String projectContextPath;

    private boolean enableInfoFunction;

    private boolean enableStaticResourceFunction;
    private String staticResourcesPrefix;
    private Path staticResourceDirectory;

    private boolean enableUrlProjectContextPrefix;
    private Path projectPath;
    private String projectName;

    private ProjectConfiguration()
    {
    }

    /**
     * @return The project level context mapping in the URL, for example <code>/gateway/<projectContextPath>/my-function</code>
     */
    public String getProjectContextPath()
    {
        return projectContextPath;
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

    public boolean enableStaticResourceFunction()
    {
        return enableStaticResourceFunction;
    }

    public String getStaticResourcesContext()
    {
        return staticResourcesPrefix;
    }

    public Path getStaticResourceDirectory()
    {
        return staticResourceDirectory;
    }

    public Path getProjectPath()
    {
        return projectPath;
    }

    public boolean enableUrlProjectContextPrefix()
    {
        return enableUrlProjectContextPrefix;
    }

    public static ProjectConfigurationBuilder builder(String rootContextPath, Path projectPath)
    {
        return new ProjectConfigurationBuilder(rootContextPath, projectPath);
    }

    public String getProjectName()
    {
        return projectName;
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

    @Override
    public String toString()
    {
        return "ProjectConfiguration{" +
                "rootContextPath='" + rootContextPath + '\'' +
                ", projectContextPath='" + projectContextPath + '\'' +
                ", enableInfoFunction=" + enableInfoFunction +
                ", enableStaticResourceFunction=" + enableStaticResourceFunction +
                ", staticResourcesPrefix='" + staticResourcesPrefix + '\'' +
                ", staticResourceDirectory=" + staticResourceDirectory +
                ", enableUrlProjectContextPrefix=" + enableUrlProjectContextPrefix +
                ", projectPath=" + projectPath +
                '}';
    }

    public static final class ProjectConfigurationBuilder
    {
        private String rootContextPath;
        private Path projectPath;

        private String projectName;
        private String projectContextPath;
        private boolean enableInfoFunction;
        private boolean enableStaticResourceFunction;
        private boolean enableUrlProjectContextPrefix;

        private String staticResourcesPrefix;
        private Path staticResourceDirectory;

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
            projectConfiguration.projectPath = this.projectPath;
            projectConfiguration.staticResourcesPrefix = this.staticResourcesPrefix;
            projectConfiguration.projectContextPath = this.projectContextPath;
            projectConfiguration.staticResourceDirectory = this.staticResourceDirectory;
            projectConfiguration.enableInfoFunction = this.enableInfoFunction;
            projectConfiguration.enableStaticResourceFunction = this.enableStaticResourceFunction;
            projectConfiguration.enableUrlProjectContextPrefix = this.enableUrlProjectContextPrefix;
            projectConfiguration.projectName = this.projectName;
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

                staticResourcesPrefix = p.getProperty("functions.static.prefix", staticResourcesPrefix);

                projectName = p.getProperty("project.name", projectName);
                projectContextPath = p.getProperty("project.context-path", projectContextPath);
                enableUrlProjectContextPrefix = Boolean.parseBoolean(p.getProperty("mapping.use-project-context-path", "true"));
            }
            return this;
        }
    }
}
