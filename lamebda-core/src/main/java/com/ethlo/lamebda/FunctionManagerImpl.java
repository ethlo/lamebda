package com.ethlo.lamebda;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.compiler.LamebdaCompiler;
import com.ethlo.lamebda.compiler.groovy.GroovyCompiler;
import com.ethlo.lamebda.compiler.java.JavaCompiler;
import com.ethlo.lamebda.generator.GeneratorHelper;
import com.ethlo.lamebda.lifecycle.ProjectClosingEvent;
import com.ethlo.lamebda.lifecycle.ProjectLoadedEvent;
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

    public static final String PROJECT_FILENAME = "project.properties";
    public static final String DEFAULT_CONFIG_FILENAME = "application.properties";
    public static final String API_SPECIFICATION_YAML_FILENAME = "oas.yaml";

    public static final String JAR_EXTENSION = "jar";
    public static final String GROOVY_EXTENSION = "groovy";
    public static final String JAVA_EXTENSION = "java";
    public static final String PROPERTIES_EXTENSION = "properties";

    public static final String SPECIFICATION_DIRECTORY = "specification";
    public static final String LIB_DIRECTORY = "lib";

    private final ProjectConfiguration projectConfiguration;
    private final ApplicationContext parentContext;
    private AnnotationConfigApplicationContext projectCtx;

    private final GeneratorHelper generatorHelper;
    private LinkedList<LamebdaCompiler> compilers;
    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    public FunctionManagerImpl(ApplicationContext parentContext, ProjectConfiguration projectConfiguration)
    {
        Assert.notNull(parentContext, "parentContext cannot be null");
        Assert.notNull(projectConfiguration, "projectConfiguration cannot be null");

        this.projectConfiguration = projectConfiguration;
        this.parentContext = parentContext;

        final Path projectPath = projectConfiguration.getPath();
        if (!Files.exists(projectPath))
        {
            throw new UncheckedIOException(new FileNotFoundException("Cannot use " + projectPath.toAbsolutePath() + " as project directory as it does not exist"));
        }

        logger.debug("ProjectConfiguration: {}", projectConfiguration.toPrettyString());

        try
        {
            Files.createDirectories(projectConfiguration.getTargetClassDirectory());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to create target class directory: " + projectConfiguration.getTargetClassDirectory(), e);
        }

        for (URL cpUrl : projectConfiguration.getClassPath())
        {
            groovyClassLoader.addURL(cpUrl);
        }

        decompressIfApplicable(projectPath);

        setupCompilers();

        addLibraries();

        final Path apiPath = projectConfiguration.getSpecificationPath().resolve(API_SPECIFICATION_YAML_FILENAME);
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

        initialize();
    }

    private void setupCompilers()
    {
        this.compilers = new LinkedList<>();
        compilers.add(new JavaCompiler(new SimpleThrowawayClassLoader(groovyClassLoader), projectConfiguration.getJavaSourcePaths()));
        compilers.add(new GroovyCompiler(groovyClassLoader, projectConfiguration.getGroovySourcePaths()));
    }

    private void decompressIfApplicable(final Path projectPath)
    {
        final Path archivePath = projectPath.resolve(projectPath.getFileName() + "." + JAR_EXTENSION);
        if (Files.exists(archivePath))
        {
            decompress(projectPath, archivePath);
        }
    }

    private void decompress(final Path projectPath, final Path archivePath)
    {
        try
        {
            unzipDirectory(archivePath, projectPath);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void addLibraries()
    {
        final Path libPath = projectConfiguration.getLibraryPath();
        if (Files.isDirectory(libPath))
        {
            getLibUrls().forEach(url ->
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

    private void unzipDirectory(final Path archivePath, Path targetDir) throws IOException
    {
        final ZipFile zipFile = new ZipFile(archivePath.toString());
        final Enumeration zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements())
        {
            final ZipEntry ze = ((ZipEntry) zipEntries.nextElement());
            final Path target = targetDir.resolve(ze.getName());
            if (!ze.isDirectory())
            {
                Files.createDirectories(target.getParent());
                final InputStream in = zipFile.getInputStream(ze);
                final boolean overwrite = !target.getFileName().toString().endsWith("." + PROPERTIES_EXTENSION);
                final boolean exists = Files.exists(target);
                if (!exists || overwrite)
                {
                    logger.debug("Unpacking {} to {}", ze.getName(), target);
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                else
                {
                    logger.info("Not overwriting {}", target);
                }
            }
        }
    }

    private List<String> getLibUrls()
    {
        if (!Files.exists(projectConfiguration.getLibraryPath(), LinkOption.NOFOLLOW_LINKS))
        {
            return Collections.emptyList();
        }
        return IoUtil.toClassPathList(projectConfiguration.getLibraryPath());
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
        if (genFile.isPresent())
        {
            final String[] args = genFile.get().split(" ");
            generatorHelper.generate(projectConfiguration.getPath(), args);
        }
        else
        {
            logger.info("No " + generationCommandFile + " file found on classpath. Generation skipped");
        }
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
        final Set<String> basePackages = projectConfiguration.getBasePackages();
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

        final Path configFilePath = projectConfiguration.getPath().resolve(DEFAULT_CONFIG_FILENAME);
        if (Files.exists(configFilePath))
        {
            propertyPlaceholderConfigurer.setLocation(new FileSystemResource(configFilePath));
        }

        this.projectCtx = new AnnotationConfigApplicationContext();
        this.projectCtx.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
        this.projectCtx.setParent(parentContext);
        this.projectCtx.setAllowBeanDefinitionOverriding(false);
        this.projectCtx.setClassLoader(groovyClassLoader);
        this.projectCtx.setId(projectConfiguration.getName());
    }

    private void addResourceClasspath()
    {
        final URL mainResourcesUrl = IoUtil.toURL(projectConfiguration.getMainResourcePath());
        logger.debug("Adding main resources to classpath: {}", mainResourcesUrl);
        groovyClassLoader.addURL(mainResourcesUrl);

        final URL targetResourcesUrl = IoUtil.toURL(projectConfiguration.getTargetClassDirectory());
        logger.debug("Adding main classes to classpath: {}", targetResourcesUrl);
        groovyClassLoader.addURL(targetResourcesUrl);
    }

    private void createProjectConfigBean()
    {
        final ConfigurableListableBeanFactory bf = projectCtx.getBeanFactory();
        bf.registerSingleton("projectConfiguration", projectConfiguration);
    }

    private void compileSources()
    {
        final Path classesDir = projectConfiguration.getTargetClassDirectory();
        for (LamebdaCompiler compiler : compilers)
        {
            compiler.compile(classesDir);
        }
    }

    private void apiSpecProcessing()
    {
        final Path apiPath = projectConfiguration.getSpecificationPath().resolve(API_SPECIFICATION_YAML_FILENAME);
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
            groovyClassLoader.addClasspath(modelPath.toAbsolutePath().toString());
            logger.debug("Added model classpath {}", modelPath);
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
    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
    }
}
