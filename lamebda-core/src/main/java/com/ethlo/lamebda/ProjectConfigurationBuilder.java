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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.ethlo.lamebda.util.IoUtil;

public class ProjectConfigurationBuilder
{
    private final String rootContextPath;
    private final Path projectPath;
    private String projectName;
    private String projectContextPath;
    private boolean enableUrlProjectContextPrefix;
    private String projectVersion;
    private String apiDocGenerator;
    private boolean listenForChanges;
    private Set<String> basePackages = Collections.emptySet();

    private Set<Path> javaSourcePaths;
    private Set<Path> groovySourcePaths;
    private Set<URL> classPaths;

    protected ProjectConfigurationBuilder(String rootContextPath, Path projectPath)
    {
        this.rootContextPath = rootContextPath;
        this.projectPath = projectPath.toAbsolutePath();

        // Set defaults
        this.apiDocGenerator = "html";

        this.projectName = projectPath.getFileName().toString();
        this.projectContextPath = projectPath.getFileName().toString();
        this.enableUrlProjectContextPrefix = true;
        this.listenForChanges = true;

        final Path mainPath = projectPath.resolve("src").resolve("main");
        this.javaSourcePaths = new LinkedHashSet<>(Collections.singleton(mainPath.resolve("java")));
        this.groovySourcePaths = new LinkedHashSet<>(Collections.singleton(mainPath.resolve("groovy")));
        this.classPaths = new LinkedHashSet<>(Collections.singleton(IoUtil.toURL(projectPath.resolve("target").resolve("classes"))));
    }

    public ProjectConfigurationBuilder basePackages(String... basePackages)
    {
        this.basePackages = new TreeSet<>(Arrays.asList(basePackages));
        return this;
    }

    public ProjectConfigurationBuilder projectContextPath(String projectContextPath)
    {
        this.projectContextPath = projectContextPath;
        return this;
    }

    public ProjectConfigurationBuilder enableUrlProjectContextPrefix(boolean enableUrlProjectContextPrefix)
    {
        this.enableUrlProjectContextPrefix = enableUrlProjectContextPrefix;
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
        final Path projectConfigFile = projectPath.resolve(FunctionManagerImpl.PROJECT_FILENAME);
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

            apiDocGenerator = p.getProperty("specification.api.doc.generator", apiDocGenerator);

            listenForChanges = Boolean.parseBoolean(p.getProperty("system.listen-for-changes", Boolean.toString(listenForChanges)));

            final String legacyBasePackages = p.getProperty("system.base-packages");
            final String projectBasePackages = p.getProperty("project.base-packages");
            final String basePackages = projectBasePackages != null ? projectBasePackages : legacyBasePackages;
            if (basePackages != null)
            {
                this.basePackages = StringUtils.commaDelimitedListToSet(basePackages);
            }

            final String javaSrcDirs = p.getProperty("project.src.java");
            if (javaSrcDirs != null)
            {
                javaSourcePaths.addAll(StringUtils.commaDelimitedListToSet(javaSrcDirs).stream().map(projectPath::resolve).collect(Collectors.toSet()));
            }

            final String groovySrcDirs = p.getProperty("project.src.groovy");
            if (groovySrcDirs != null)
            {
                groovySourcePaths.addAll(StringUtils.commaDelimitedListToSet(groovySrcDirs).stream().map(projectPath::resolve).collect(Collectors.toSet()));
            }

            final String additionalClassPath = p.getProperty("project.classpath");
            if (additionalClassPath != null)
            {
                this.classPaths.addAll(StringUtils.commaDelimitedListToSet(additionalClassPath).stream().map(IoUtil::toURL).collect(Collectors.toSet()));
            }
        }
        return this;
    }

    public String getRootContextPath()
    {
        return rootContextPath;
    }

    public Path getProjectPath()
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

    public boolean isEnableUrlProjectContextPrefix()
    {
        return enableUrlProjectContextPrefix;
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

    public Set<String> getBasePackages()
    {
        return basePackages;
    }

    public Set<Path> getJavaSourcePaths()
    {
        return javaSourcePaths;
    }

    public Set<Path> getGroovySourcePaths()
    {
        return groovySourcePaths;
    }

    public Set<URL> getClassPath()
    {
        return classPaths;
    }

    public ProjectConfigurationBuilder addJavaSourcePath(final Path path)
    {
        javaSourcePaths.add(projectPath.resolve(path));
        return this;
    }

    public ProjectConfigurationBuilder addgroovySourcePath(final Path path)
    {
        groovySourcePaths.add(projectPath.resolve(path));
        return this;
    }
}
