package com.ethlo.lamebda;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.compiler.GroovyCompiler;
import com.ethlo.lamebda.compiler.JavaCompiler;
import com.ethlo.lamebda.generator.GeneratorHelper;
import com.ethlo.lamebda.lifecycle.ProjectClosingEvent;
import com.ethlo.lamebda.lifecycle.ProjectLoadedEvent;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
import com.ethlo.lamebda.util.IoUtil;
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

public class FunctionManagerImpl implements FunctionManager
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionManagerImpl.class);
    private final ProjectConfiguration projectConfiguration;
    private final ApplicationContext parentContext;
    private AnnotationConfigApplicationContext projectCtx;

    private LamebdaResourceLoader lamebdaResourceLoader;

    private final GeneratorHelper generatorHelper;

    public FunctionManagerImpl(ApplicationContext parentContext, LamebdaResourceLoader lamebdaResourceLoader)
    {
        Assert.notNull(parentContext, "parentContext cannot be null");
        Assert.notNull(parentContext, "lamebdaResourceLoader cannot be null");

        this.projectConfiguration = lamebdaResourceLoader.getProjectConfiguration();
        final Path apiPath = projectConfiguration.getSpecificationPath().resolve(FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);
        final Path jarDir = projectConfiguration.getPath().resolve(".generator");
        if (Files.exists(jarDir))
        {
            this.generatorHelper = new GeneratorHelper(projectConfiguration.getJavaCmd(), jarDir);
        }
        else if (Files.exists(apiPath))
        {
            logger.info("Found specification file in {}, but there are no generator libraries in {}. Skipping.", apiPath, jarDir);
            generatorHelper = null;
        }
        else
        {
            generatorHelper = null;
        }

        this.lamebdaResourceLoader = lamebdaResourceLoader;
        this.parentContext = parentContext;

        initialize();
    }

    private void generateModels() throws IOException
    {
        if (generatorHelper != null)
        {
            runRegen(projectConfiguration, ".models.gen");
        }
    }

    private void generateHumanReadableApiDoc(final ProjectConfiguration projectConfiguration) throws IOException
    {
        if (generatorHelper != null)
        {
            runRegen(projectConfiguration, ".apidoc.gen");
        }
    }

    private void runRegen(final ProjectConfiguration projectConfiguration, final String generationCommandFile) throws IOException
    {
        final Optional<String> genFile = IoUtil.toString(projectConfiguration.getPath().resolve(generationCommandFile));
        final Optional<String> defaultGenFile = IoUtil.toString("/generation/" + generationCommandFile);
        final String[] args = genFile.map(s -> s.split(" ")).orElseGet(() -> defaultGenFile.get().split(" "));
        generatorHelper.generate(projectConfiguration.getPath(), args);
    }

    private void initialize()
    {
        setupSpringChildContext();
        addResourceClasspath();
        apiSpecProcessing();
        createProjectConfigBean();
        compileSources();
        findBeans();

        parentContext.publishEvent(new ProjectLoadedEvent(projectConfiguration, projectCtx));
    }

    private void findBeans()
    {
        // Scan for beans
        final List<String> basePackages = projectConfiguration.getBasePackages();
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
        final PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();

        final Path configFilePath = projectConfiguration.getPath().resolve(FileSystemLamebdaResourceLoader.DEFAULT_CONFIG_FILENAME);
        if (Files.exists(configFilePath))
        {
            propertyPlaceholderConfigurer.setLocation(new FileSystemResource(configFilePath));
        }

        this.projectCtx = new AnnotationConfigApplicationContext();
        this.projectCtx.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
        this.projectCtx.setParent(parentContext);
        this.projectCtx.setAllowBeanDefinitionOverriding(false);
        this.projectCtx.setClassLoader(lamebdaResourceLoader.getClassLoader());
        this.projectCtx.setId(projectConfiguration.getName());
    }

    private void addResourceClasspath()
    {
        final GroovyClassLoader gcl = lamebdaResourceLoader.getClassLoader();
        final URL mainResourcesUrl = IoUtil.toURL(projectConfiguration.getMainResourcePath());
        logger.info("Adding main resources to classpath: {}", mainResourcesUrl);
        gcl.addURL(mainResourcesUrl);

        final URL targetResourcesUrl = IoUtil.toURL(projectConfiguration.getTargetClassDirectory());
        logger.info("Adding main classes to classpath: {}", targetResourcesUrl);
        gcl.addURL(targetResourcesUrl);
    }

    private void createProjectConfigBean()
    {
        final ConfigurableListableBeanFactory bf = projectCtx.getBeanFactory();
        bf.registerSingleton("projectConfiguration", lamebdaResourceLoader.getProjectConfiguration());
    }

    private void compileSources()
    {
        final Path classesDir = projectConfiguration.getTargetClassDirectory();
        compileGroovy(classesDir);
        compileJava(classesDir);
    }

    private void compileGroovy(Path classesDir)
    {
        final Set<Path> sourcePaths = new TreeSet<>(Arrays.asList(IoUtil.exists(getProjectConfiguration().getGroovySourcePath(), getProjectConfiguration().getPath().resolve("target").resolve("generated-sources").resolve("models"))));

        if (!sourcePaths.isEmpty())
        {
            logger.info("Compiling groovy sources in {}", StringUtils.collectionToCommaDelimitedString(sourcePaths));
            final GroovyClassLoader classLoader = lamebdaResourceLoader.getClassLoader();
            final GroovyCompiler groovyCompiler = new GroovyCompiler(classLoader, sourcePaths);
            groovyCompiler.compile(classesDir);
        }
        else
        {
            logger.info("No groovy sources to compile");
        }
    }

    private void compileJava(Path classesDir)
    {
        final Path javaSourcePath = getProjectConfiguration().getJavaSourcePath();
        if (Files.isDirectory(javaSourcePath))
        {
            logger.info("Compiling java sources in {}", javaSourcePath);
            final GroovyClassLoader classLoader = lamebdaResourceLoader.getClassLoader();
            final JavaCompiler jp = new JavaCompiler(classLoader, javaSourcePath);
            jp.compile(classesDir);
        }
        else
        {
            logger.info("No java sources to compile");
        }
    }

    private void apiSpecProcessing()
    {
        final Path apiPath = projectConfiguration.getSpecificationPath().resolve(FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);
        final Path targetPath = projectConfiguration.getPath().resolve("target");

        if (Files.exists(apiPath))
        {
            try
            {
                final Path marker = targetPath.resolve(".api_lastgenerated");
                final OffsetDateTime specModified = lastModified(apiPath);
                final OffsetDateTime modelModified = lastModified(marker);

                if (specModified.isAfter(modelModified))
                {
                    generateModels();

                    generateHumanReadableApiDoc(projectConfiguration);
                }

                setLastModified(targetPath, marker, specModified);
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }

        final Path modelPath = projectConfiguration.getPath().resolve("target").resolve("generated-sources").resolve("models").toAbsolutePath();
        if (Files.exists(modelPath))
        {
            lamebdaResourceLoader.addClasspath(modelPath.toAbsolutePath().toString());
            logger.info("Added model classpath {}", modelPath);
        }
    }

    private void setLastModified(final Path targetPath, final Path marker, final OffsetDateTime specModified) throws IOException
    {
        if (Files.exists(targetPath))
        {
            try
            {
                Files.createFile(marker);

            }
            catch (FileAlreadyExistsException exc)
            {
                // Ignore
            }

            Files.setLastModifiedTime(marker, FileTime.from(specModified.toInstant()));
        }
    }

    private OffsetDateTime lastModified(final Path path) throws IOException
    {
        if (Files.exists(path))
        {
            return OffsetDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.UTC);
        }
        return OffsetDateTime.MIN;
    }

    @Override
    public void close()
    {
        parentContext.publishEvent(new ProjectClosingEvent(projectConfiguration, projectCtx));

        try
        {
            projectCtx.close();
            this.lamebdaResourceLoader.close();
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
    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
    }
}
