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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.security.UsernamePasswordCredentials;

public class ProjectConfigurationBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectConfigurationBuilder.class);

    private UsernamePasswordCredentials adminCredentials;
    private String rootContextPath;
    private String projectPath;

    private String projectName;
    private String projectContextPath;
    private boolean enableInfoFunction;
    private boolean isInfoProtected = true;
    private boolean enableStaticResourceFunction;
    private boolean enableUrlProjectContextPrefix;

    private String staticResourcesPrefix;
    private Path staticResourceDirectory;
    private String projectVersion;

    private String apiDocGenerator;

    private boolean listenForChanges;
    private List<String> basePackages = Collections.emptyList();

    protected ProjectConfigurationBuilder(String rootContextPath, Path projectPath)
    {
        this.rootContextPath = rootContextPath;
        this.projectPath = projectPath.toAbsolutePath().toString();

        // Set defaults
        this.apiDocGenerator = "html";

        this.projectName = projectPath.getFileName().toString();
        this.projectContextPath = projectPath.getFileName().toString();
        this.enableInfoFunction = true;
        this.enableStaticResourceFunction = true;
        this.enableUrlProjectContextPrefix = true;
        this.staticResourcesPrefix = "static";
        this.staticResourceDirectory = projectPath.resolve(FileSystemLamebdaResourceLoader.STATIC_DIRECTORY);
        this.listenForChanges = true;
    }

    public ProjectConfigurationBuilder basePackages(String... basePackages)
    {
        this.basePackages = Arrays.asList(basePackages);
        return this;
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

    public ProjectConfigurationBuilder listenForChanges(boolean listenForChanges)
    {
        this.listenForChanges = listenForChanges;
        return this;
    }

    public ProjectConfiguration build()
    {
        return new ProjectConfiguration(this);
    }

    public ProjectConfigurationBuilder loadIfExists()
    {
        final Path projectConfigFile = Paths.get(projectPath).resolve(FileSystemLamebdaResourceLoader.PROJECT_FILENAME);
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
            staticResourceDirectory = Paths.get(p.getProperty("function.static.path", staticResourceDirectory.toString()));

            apiDocGenerator = p.getProperty("specification.api.doc.generator", apiDocGenerator);

            final String adminUsername = p.getProperty("admin.credentials.username", "admin");
            String adminPassword = p.getProperty("admin.credentials.password");
            if (adminPassword == null)
            {
                adminPassword = generateRandomString(new SecureRandom(), 12);
                logger.info("Using generated admin password: {}. Please set it using 'admin.credentials.password=' in project.properties", adminPassword);
            }
            adminCredentials = new UsernamePasswordCredentials(adminUsername, adminPassword);

            // Info function
            isInfoProtected = Boolean.parseBoolean(p.getProperty("functions.info.protected", Boolean.toString(isInfoProtected)));
            enableInfoFunction = Boolean.parseBoolean(p.getProperty("functions.info.enabled", Boolean.toString(enableInfoFunction)));

            listenForChanges = Boolean.parseBoolean(p.getProperty("system.listen-for-changes", Boolean.toString(listenForChanges)));
            basePackages = new ArrayList<>(StringUtils.commaDelimitedListToSet(p.getProperty("system.base-packages", "service")));
        }
        return this;
    }

    private static String generateRandomString(Random random, int length)
    {
        return random.ints(48, 122)
                .filter(i -> (i < 57 || i > 65) && (i < 90 || i > 97))
                .mapToObj(i -> (char) i)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }


    public UsernamePasswordCredentials getAdminCredentials()
    {
        return adminCredentials;
    }

    public String getRootContextPath()
    {
        return rootContextPath;
    }

    public String getProjectPath()
    {
        return projectPath;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public String getProjectContextPath()
    {
        return projectContextPath;
    }

    public boolean isEnableInfoFunction()
    {
        return enableInfoFunction;
    }

    public boolean isEnableStaticResourceFunction()
    {
        return enableStaticResourceFunction;
    }

    public boolean isEnableUrlProjectContextPrefix()
    {
        return enableUrlProjectContextPrefix;
    }

    public String getStaticResourcesPrefix()
    {
        return staticResourcesPrefix;
    }

    public Path getStaticResourceDirectory()
    {
        return staticResourceDirectory;
    }

    public String getProjectVersion()
    {
        return projectVersion;
    }

    public String getApiDocGenerator()
    {
        return apiDocGenerator;
    }

    public boolean isListenForChanges()
    {
        return listenForChanges;
    }

    public boolean isInfoProtected()
    {
        return isInfoProtected;
    }

    public List<String> getBasePackages()
    {
        return basePackages;
    }
}
