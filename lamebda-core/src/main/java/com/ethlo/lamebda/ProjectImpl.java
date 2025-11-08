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
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.lifecycle.ProjectClosingEvent;
import com.ethlo.lamebda.lifecycle.ProjectLoadedEvent;
import com.ethlo.lamebda.util.IoUtil;
import com.ethlo.qjc.groovy.GroovyCompiler;
import com.ethlo.qjc.java.JavaCompiler;
import groovy.lang.GroovyClassLoader;

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
    public static final String GROOVY_EXTENSION = "groovy";
    public static final String JAVA_EXTENSION = "java";
    public static final String PROPERTIES_EXTENSION = "properties";
    public static final String LIB_DIRECTORY = "lib";
    private static final Logger logger = LoggerFactory.getLogger(ProjectImpl.class);
    private String alias;
    private final BootstrapConfiguration bootstrapConfiguration;
    private final ApplicationContext parentContext;
    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
    private final Path workDir;
    private final Path projectPath;
    private final Path classesDir;
    private final ProjectConfiguration projectConfiguration;
    private AnnotationConfigApplicationContext projectCtx;
    private JavaCompiler javaCompiler;
    private GroovyCompiler groovyCompiler;

    public ProjectImpl(final String alias, ApplicationContext parentContext, BootstrapConfiguration bootstrapConfiguration, final Path workDirectory)
    {
        this.alias = alias;
        this.bootstrapConfiguration = Objects.requireNonNull(bootstrapConfiguration);
        this.parentContext = Objects.requireNonNull(parentContext);
        this.projectPath = bootstrapConfiguration.getPath();
        this.workDir = Objects.requireNonNull(workDirectory).toAbsolutePath();
        this.classesDir = createDirectory(workDir.resolve("target").resolve("classes"));

        final Path projectPath = bootstrapConfiguration.getPath();
        if (!Files.exists(projectPath))
        {
            throw new UncheckedIOException(new FileNotFoundException("Cannot use " + projectPath.toAbsolutePath() + " as project directory as it does not exist"));
        }

        decompressIfApplicable();
        logger.info("Using work directory {}", workDir);
        addLibraries(workDir);

        this.projectConfiguration = ProjectConfiguration.load(bootstrapConfiguration, workDir);

        readVersionFile(projectPath);
        readVersionFile(workDir);
        logger.info("Version: {}", projectConfiguration.getProjectInfo().getVersion());

        logger.debug("ProjectConfiguration: {}", projectConfiguration.toPrettyString());

        // Add manually added class-path entries
        for (URI cpUrl : projectConfiguration.getClasspath())
        {
            try
            {
                groovyClassLoader.addURL(cpUrl.toURL());
            }
            catch (MalformedURLException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        setupCompilers();
        initialize();
    }

    private Path createDirectory(final Path path)
    {
        try
        {
            Files.createDirectories(path);
            return path;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to create directory: " + path, e);
        }
    }

    private void readVersionFile(final Path path)
    {
        final Optional<String> optVersion = IoUtil.toString(path.resolve("version"));
        optVersion.ifPresent(versionStr -> projectConfiguration.getProjectInfo().setVersion(versionStr.replaceAll("^[\r\n]+|[\r\n]+$", "")));
    }

    private void setupCompilers()
    {
        javaCompiler = new JavaCompiler(new SimpleThrowawayClassLoader(groovyClassLoader));
        groovyCompiler = new GroovyCompiler(groovyClassLoader);
    }

    private void decompressIfApplicable()
    {
        final Path archivePath = projectPath.resolve(projectPath.getFileName() + "." + JAR_EXTENSION);
        logger.info("Looking for project archive file at {}", archivePath);
        if (Files.exists(archivePath))
        {
            logger.info("Decompressing project archive file {}", archivePath);
            try
            {
                unzipDirectory(archivePath, workDir);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
        else
        {
            logger.info("No project archive file found at {}", archivePath);
        }
    }

    private void addLibraries(Path path)
    {
        final Path libPath = path.resolve(ProjectImpl.LIB_DIRECTORY);
        if (Files.isDirectory(libPath))
        {
            getLibUrls(libPath).forEach(url ->
            {
                groovyClassLoader.addClasspath(url);
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

    private List<String> getLibUrls(Path path)
    {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))
        {
            return Collections.emptyList();
        }
        return IoUtil.toClassPathList(path);
    }

    private void initialize()
    {
        setupSpringChildContext();
        addResourceClasspath();
        createProjectConfigBean();
        compileSources();
        findBeans();

        parentContext.publishEvent(new ProjectLoadedEvent(projectConfiguration, projectCtx));
    }

    private void findBeans()
    {
        // Scan for beans
        final Set<String> basePackages = projectConfiguration.getProjectInfo().getBasePackages();
        if (!basePackages.isEmpty())
        {
            logger.info("Scanning base packages: {}", StringUtils.collectionToCommaDelimitedString(basePackages));
            this.projectCtx.scan(basePackages.toArray(new String[0]));
        }
        else
        {
            logger.warn("No base-packages set to scan");
        }

        projectCtx.refresh();
    }

    private void setupSpringChildContext()
    {
        this.projectCtx = new AnnotationConfigApplicationContext();
        this.projectCtx.setParent(parentContext);
        this.projectCtx.setAllowBeanDefinitionOverriding(false);
        this.projectCtx.setClassLoader(groovyClassLoader);
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

    private void addResourceClasspath()
    {
        final URL mainResourcesUrl = IoUtil.toURL(projectConfiguration.getMainResourcePath());
        logger.debug("Adding main resources to classpath: {}", mainResourcesUrl);
        groovyClassLoader.addURL(mainResourcesUrl);

        final URL targetResourcesUrl = IoUtil.toURL(classesDir);
        logger.debug("Adding main classes to classpath: {}", targetResourcesUrl);
        groovyClassLoader.addURL(targetResourcesUrl);
    }

    private void createProjectConfigBean()
    {
        final ConfigurableListableBeanFactory bf = projectCtx.getBeanFactory();
        bf.registerSingleton("projectConfiguration", bootstrapConfiguration);
    }

    private void compileSources()
    {
        javaCompiler.compile(projectConfiguration.getJavaSourcePaths(), projectConfiguration.getTargetClassDirectory());
        groovyCompiler.compile(projectConfiguration.getGroovySourcePaths(), projectConfiguration.getTargetClassDirectory());
    }

    @Override
    public void close()
    {
        parentContext.publishEvent(new ProjectClosingEvent(projectConfiguration, projectCtx));

        try
        {
            projectCtx.close();
            groovyClassLoader.close();
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
    public Optional<Resource> getApiSpecification()
    {
        final Resource resource = getProjectContext().getResource(projectConfiguration.getApiSpecificationSource());
        return Optional.ofNullable(resource.exists() ? resource : null);
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
