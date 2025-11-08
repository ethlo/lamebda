package com.ethlo.lamebda;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.lifecycle.ProjectClosingEvent;
import com.ethlo.lamebda.lifecycle.ProjectLoadedEvent;
import com.ethlo.lamebda.util.IoUtil;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 Morten Haraldsen (ethlo)
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

public class ProjectImpl implements Project
{
    public static final String PROJECT_FILENAME = "project.properties";
    public static final String DEFAULT_CONFIG_FILENAME = "application.properties";
    public static final String JAR_EXTENSION = "jar";
    public static final String PROPERTIES_EXTENSION = "properties";
    public static final String LIB_DIRECTORY = "lib";
    private static final Logger logger = LoggerFactory.getLogger(ProjectImpl.class);
    private final String alias;
    private final BootstrapConfiguration bootstrapConfiguration;
    private final ApplicationContext parentContext;
    private final URLClassLoader classLoader;
    private final Path workDir;
    private final Path projectPath;
    private final ProjectConfiguration projectConfiguration;
    private AnnotationConfigApplicationContext projectCtx;

    public ProjectImpl(final String alias, ApplicationContext parentContext, BootstrapConfiguration bootstrapConfiguration, final Path workDir)
    {
        this.alias = alias;
        this.bootstrapConfiguration = Objects.requireNonNull(bootstrapConfiguration);
        this.parentContext = Objects.requireNonNull(parentContext);
        this.projectPath = bootstrapConfiguration.getPath();
        this.workDir = workDir;

        final Path projectPath = bootstrapConfiguration.getPath();
        if (!Files.exists(projectPath))
        {
            throw new UncheckedIOException(new FileNotFoundException("Cannot use " + projectPath.toAbsolutePath() + " as project directory as it does not exist"));
        }

        decompressArchive();

        this.projectConfiguration = ProjectConfiguration.load(bootstrapConfiguration, workDir);

        readVersionFile(projectPath);
        readVersionFile(workDir);

        logger.info("ProjectConfiguration: {}", projectConfiguration.toPrettyString());

        final URL[] extraUrls = getExtraClasspathUrls();
        this.classLoader = new URLClassLoader(extraUrls, parentContext.getClassLoader());

        initialize();
    }

    private URL[] getExtraClasspathUrls()
    {
        final Set<URI> allExtraLibs = projectConfiguration.getClasspath();
        findLibraries(workDir, allExtraLibs);

        return allExtraLibs.stream().map(spec ->
        {
            try
            {
                return spec.toURL();
            }
            catch (MalformedURLException e)
            {
                throw new UncheckedIOException(e);
            }
        }).toList().toArray(new URL[0]);
    }

    private void readVersionFile(final Path path)
    {
        final Optional<String> optVersion = IoUtil.toString(path.resolve("version"));
        optVersion.ifPresent(versionStr -> projectConfiguration.getProjectInfo().setVersion(versionStr.replaceAll("^[\r\n]+|[\r\n]+$", "")));
    }

    private void decompressArchive()
    {
        final Path archivePath = projectPath.resolve(projectPath.getFileName() + "." + JAR_EXTENSION);
        logger.debug("Decompressing project archive {}", archivePath);
        try
        {
            unzipDirectory(archivePath, workDir);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void findLibraries(Path path, Set<URI> targetList)
    {
        final Path libPath = path.resolve(ProjectImpl.LIB_DIRECTORY);
        if (Files.isDirectory(libPath))
        {
            getLibUrls(libPath).forEach(url ->
            {
                targetList.add(url);
                logger.debug("Adding library classpath {}", url);
            });
        }
        else
        {
            logger.debug("Lib directory {} does not exist. Skipping", libPath);
        }
    }

    private void unzipDirectory(final Path archivePath, final Path targetDir) throws IOException
    {
        try (final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(archivePath.toFile()))))
        {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null)
            {
                if (!ze.isDirectory())
                {
                    doWrite(targetDir, zis, ze);
                }
            }
        }
    }

    private void doWrite(final Path tmpWorkDir, final ZipInputStream zis, final ZipEntry ze) throws IOException
    {
        final Path inTempTarget = tmpWorkDir.resolve(ze.getName());
        Files.createDirectories(inTempTarget.getParent());
        logger.debug("Unpacking {} to {}", ze.getName(), inTempTarget);
        Files.copy(zis, inTempTarget, StandardCopyOption.REPLACE_EXISTING);
    }

    private List<URI> getLibUrls(Path path)
    {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))
        {
            return Collections.emptyList();
        }
        try (final Stream<Path> fs = Files.list(path))
        {
            return fs.filter(p -> p.getFileName().toString().endsWith("." + ProjectImpl.JAR_EXTENSION))
                    .map(Path::toUri)
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void initialize()
    {
        setupSpringChildContext();
        createProjectConfigBean();
        findBeans();

        parentContext.publishEvent(new ProjectLoadedEvent(projectConfiguration, projectCtx));
    }

    private void findBeans()
    {
        // Scan for beans
        final Set<String> basePackages = projectConfiguration.getProjectInfo().getBasePackages();
        logger.info("Scanning base packages: {}", StringUtils.collectionToCommaDelimitedString(basePackages));
        projectCtx.scan(basePackages.toArray(new String[0]));
        projectCtx.refresh();
    }

    private void setupSpringChildContext()
    {
        this.projectCtx = new AnnotationConfigApplicationContext();
        this.projectCtx.setParent(parentContext);
        this.projectCtx.setAllowBeanDefinitionOverriding(false);
        this.projectCtx.setClassLoader(classLoader);
        this.projectCtx.setId(projectConfiguration.getProjectInfo().getName());

        final Resource[] configResources = getConfigResources();

        // Always base on a new environment that inherits parent
        final StandardEnvironment env = new StandardEnvironment();
        env.merge((ConfigurableEnvironment) parentContext.getEnvironment());

        if (configResources.length > 0)
        {
            // Load project/module-specific properties
            final PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();
            propertyConfigurer.setLocations(configResources);
            propertyConfigurer.setEnvironment(env);

            final PropertySource<Properties> propertySource = createPropertySource(configResources);
            env.getPropertySources().addFirst(propertySource);

            this.projectCtx.addBeanFactoryPostProcessor(propertyConfigurer);

            logger.debug("Added property sources from: {}", Arrays.toString(configResources));
            prettyPrint(propertySource.getSource());
        }

        // Set the fully merged environment on the child context
        this.projectCtx.setEnvironment(env);
    }

    private Resource[] getConfigResources()
    {
        return Stream.of(new FileSystemResource(workDir.resolve(DEFAULT_CONFIG_FILENAME)), new FileSystemResource(projectPath.resolve(DEFAULT_CONFIG_FILENAME))).filter(FileSystemResource::exists).toArray(Resource[]::new);
    }

    private void prettyPrint(final Properties properties)
    {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> e : properties.entrySet())
        {
            sb.append("\n").append(e.getKey()).append("=").append(e.getValue());
        }
        logger.debug("Project configuration properties: {}", sb);
    }

    private PropertySource<Properties> createPropertySource(final Resource[] configFilePath)
    {
        final Properties result = new Properties();

        for (Resource resource : configFilePath)
        {
            try (final Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)))
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

        return new PropertySource<>(StringUtils.arrayToCommaDelimitedString(configFilePath), result)
        {
            @Override
            public String getProperty(@NonNull String key)
            {
                return result.getProperty(key);
            }
        };
    }

    private void createProjectConfigBean()
    {
        final ConfigurableListableBeanFactory bf = projectCtx.getBeanFactory();
        bf.registerSingleton("projectConfiguration", bootstrapConfiguration);
    }

    @Override
    public void close()
    {
        parentContext.publishEvent(new ProjectClosingEvent(projectConfiguration, projectCtx));

        try
        {
            projectCtx.close();
            classLoader.close();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public AnnotationConfigApplicationContext getProjectContext()
    {
        return projectCtx;
    }

    @Override
    public String getAlias()
    {
        return alias;
    }

    @Override
    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
    }
}
