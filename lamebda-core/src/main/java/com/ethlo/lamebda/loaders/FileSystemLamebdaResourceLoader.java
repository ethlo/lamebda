package com.ethlo.lamebda.loaders;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.ethlo.lamebda.ApiSpecificationModificationNotice;
import com.ethlo.lamebda.FunctionContextAware;
import com.ethlo.lamebda.FunctionModificationNotice;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.PropertyFile;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.FileSystemEvent;
import com.ethlo.lamebda.io.WatchDir;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.FileNameUtil;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class FileSystemLamebdaResourceLoader implements LamebdaResourceLoader
{
    private static final Logger logger = LoggerFactory.getLogger(FileSystemLamebdaResourceLoader.class);

    public static final String PROJECT_FILENAME = "project.properties";
    public static final String API_SPECIFICATION_YAML_FILENAME = "oas.yaml";
    private static final String API_SPECIFICATION_JSON_FILENAME = "oas.json";
    private static final String DEFAULT_CONFIG_FILENAME = "config.properties";

    private static final String SCRIPT_EXTENSION = "groovy";
    private static final String JAR_EXTENSION = "jar";

    public static final String SCRIPT_DIRECTORY = "scripts";
    public static final String STATIC_DIRECTORY = "static";
    public static final String SPECIFICATION_DIRECTORY = "specification";
    public static final String SHARED_DIRECTORY = "shared";
    private static final String LIB_DIRECTORY = "lib";

    private final Path projectPath;
    private final Path scriptPath;
    private final Path specificationPath;
    private final Path sharedPath;
    private final Path libPath;

    private final FunctionSourcePreProcessor functionSourcePreProcessor;
    private final FunctionPostProcessor functionPostProcessor;
    private final ProjectConfiguration projectConfiguration;

    private Consumer<FunctionModificationNotice> functionChangeListener;
    private Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener;
    private Consumer<FileSystemEvent> libChangeListener;
    private Consumer<FileSystemEvent> projectChangeListener;

    private WatchDir watchDir;

    public FileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration) throws IOException
    {
        this(projectConfiguration, (c, s) -> s, s -> s);
    }

    public FileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration, FunctionSourcePreProcessor functionSourcePreProcessor, FunctionPostProcessor functionPostProcessor) throws IOException
    {
        // TODO: This seems to be unstable, so leaving it off for now
        //configureLogback(projectConfiguration.getPath());

        this.projectConfiguration = projectConfiguration;
        this.functionSourcePreProcessor = Assert.notNull(functionSourcePreProcessor, "functionSourcePreProcesor cannot be null");
        this.functionPostProcessor = Assert.notNull(functionPostProcessor, "functionPostProcessor cannot be null");

        final Path projectPath = projectConfiguration.getPath();
        if (!Files.exists(projectPath))
        {
            throw new FileNotFoundException("Cannot use " + projectPath.toAbsolutePath() + " as project directory as it does not exist");
        }

        this.projectPath = projectPath;
        this.scriptPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SCRIPT_DIRECTORY));
        this.specificationPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SPECIFICATION_DIRECTORY));
        this.sharedPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SHARED_DIRECTORY));
        this.libPath = IoUtil.ensureDirectoryExists(projectPath.resolve(LIB_DIRECTORY));

        logger.info("Project directory: {}", projectPath);
        logger.debug("HandlerFunction directory: {}", scriptPath);
        logger.debug("Specification directory: {}", specificationPath);
        logger.debug("Shared path: {}", sharedPath);
        logger.debug("Library path: {}", libPath);

        listenForChanges(projectPath, scriptPath, specificationPath, libPath);
    }

    private static void configureLogback(Path projectPath)
    {
        final Path logbackConfig = projectPath.resolve("logback.xml");
        if (! Files.exists(logbackConfig))
        {
            return;
        }

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try
        {
            configurator.doConfigure(logbackConfig.toUri().toURL());
        }
        catch (JoranException exc)
        {
            throw new IllegalArgumentException("Unable to reconfigure logback using " + logbackConfig.toString() + "logback.xml file", exc);
        }
        catch (MalformedURLException exc)
        {
            throw new UncheckedIOException("Unable to reconfigure logback using " + logbackConfig.toString() + "logback.xml file", exc);
        }
    }


    @Override
    public void setFunctionChangeListener(Consumer<FunctionModificationNotice> l)
    {
        this.functionChangeListener = l;
    }

    @Override
    public void setProjectChangeListener(Consumer<FileSystemEvent> l)
    {
        this.projectChangeListener = l;
    }

    @Override
    public void setApiSpecificationChangeListener(Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener)
    {
        this.apiSpecificationChangeListener = apiSpecificationChangeListener;
    }

    @Override
    public void setLibChangeListener(Consumer<FileSystemEvent> listener)
    {
        this.libChangeListener = listener;
    }

    @Override
    public ServerFunction load(GroovyClassLoader classLoader, Path sourcePath)
    {
        final Class<ServerFunction> clazz = parseClass(classLoader, sourcePath);
        final ServerFunction instance = instantiate(clazz);
        setContextIfApplicable(instance);
        return functionPostProcessor.process(instance);
    }

    private void setContextIfApplicable(final ServerFunction func)
    {
        if (func instanceof FunctionContextAware)
        {
            ((FunctionContextAware) func).setContext(loadContext(func.getClass()));
        }
    }

    public FunctionContext loadContext(final Class<?> functionClass)
    {
        final FunctionConfiguration functionConfiguration = new FunctionConfiguration();

        final PropertyFile propertyFile = functionClass.getAnnotation(PropertyFile.class);
        final boolean required = propertyFile != null ? propertyFile.required() : false;
        final String filename = propertyFile != null ? propertyFile.value() : FileSystemLamebdaResourceLoader.DEFAULT_CONFIG_FILENAME;
        final Path cfgFilePath = getProjectConfiguration().getPath().resolve(filename);

        if (Files.exists(cfgFilePath))
        {
            //
            try
            {
                final String cfgContent = readSource(cfgFilePath);
                functionConfiguration.load(new StringReader(cfgContent));
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }
        else if (required)
        {
            // Does not exist, but is required
            throw new UncheckedIOException(new FileNotFoundException(cfgFilePath.toString()));
        }

        return new FunctionContext(projectConfiguration, functionConfiguration);
    }


    private ServerFunction instantiate(Class<ServerFunction> clazz)
    {
        try
        {
            return ServerFunction.class.cast(clazz.newInstance());
        }
        catch (InstantiationException | IllegalAccessException exc)
        {
            throw new IllegalStateException("Cannot instantiate class " + clazz.getName(), exc);
        }
    }

    @Override
    public Class<ServerFunction> parseClass(GroovyClassLoader classLoader, Path sourcePath)
    {
        try
        {
            final String source = readSource(sourcePath);
            final String modifiedSource = functionSourcePreProcessor.process(classLoader, source);

            final Class<?> clazz = classLoader.parseClass(modifiedSource != null ? modifiedSource : source);
            Assert.isTrue(ServerFunction.class.isAssignableFrom(clazz), "Class " + clazz.getName() + " must be instance of class ServerFunction");

            final String actualClassName = clazz.getCanonicalName();
            final String expectedClassName = toClassName(sourcePath);
            Assert.isTrue(actualClassName.equals(expectedClassName), "Unexpected class name '" + actualClassName + "' in file " + sourcePath + ". Expected " + expectedClassName);

            return (Class<ServerFunction>) clazz;
        }
        catch (IOException exc)
        {
            throw new IllegalStateException("Cannot parse class " + sourcePath, exc);
        }
    }

    private String toClassName(final Path sourcePath)
    {
        return scriptPath.relativize(sourcePath).toString().replace('/', '.').replace("." + SCRIPT_EXTENSION, "");
    }

    private void functionChanged(Path sourcePath, ChangeType changeType)
    {
        if (functionChangeListener != null)
        {
            functionChangeListener.accept(new FunctionModificationNotice(changeType, sourcePath));
        }
    }

    private void apiSpecificationChanged(Path sourcePath, ChangeType changeType)
    {
        if (apiSpecificationChangeListener != null)
        {
            apiSpecificationChangeListener.accept(new ApiSpecificationModificationNotice(changeType, sourcePath));
        }
    }

    private void listenForChanges(Path... paths) throws IOException
    {
        this.watchDir = new WatchDir(e -> {
            logger.debug("{}", e);

            try
            {
                fileChanged(e);
            }
            catch (Exception exc)
            {
                logger.warn("Error during file changed event processing: {}", exc.getMessage(), exc);
            }
        }, true, paths);

        new Thread()
        {
            @Override
            public void run()
            {
                setName("filesystem-watcher");
                logger.info("Watching {} for changes", Arrays.asList(paths));
                watchDir.processEvents();
            }
        }.start();
    }

    private void fileChanged(FileSystemEvent event)
    {
        final String filename = event.getPath().getFileName().toString();
        final ChangeType changeType = event.getChangeType();

        if (FileNameUtil.getExtension(filename).equals(SCRIPT_EXTENSION) && event.getPath().getParent().equals(scriptPath))
        {
            functionChanged(event.getPath(), changeType);
        }
        else if (FileNameUtil.getExtension(filename).equals(JAR_EXTENSION) && event.getPath().getParent().equals(libPath))
        {
            libChanged(event);
        }
        else if (filename.equals(API_SPECIFICATION_JSON_FILENAME) || filename.equals(API_SPECIFICATION_YAML_FILENAME))
        {
            apiSpecificationChanged(event.getPath(), changeType);
        }
        else if (filename.equals(PROJECT_FILENAME) && event.getPath().getParent().equals(projectPath) && changeType == ChangeType.MODIFIED)
        {
            projectConfigurationChanged(event);
        }
    }

    private void projectConfigurationChanged(FileSystemEvent e)
    {
        if (projectChangeListener != null)
        {
            projectChangeListener.accept(e);
        }
    }

    private void libChanged(FileSystemEvent event)
    {
        if (libChangeListener != null)
        {
            libChangeListener.accept(event);
        }
    }

    @Override
    public String readSource(Path sourcePath) throws IOException
    {
        return new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
    }

    @Override
    public List<ServerFunctionInfo> findAll(long offset, int size)
    {
        try
        {
            return Files.list(scriptPath).filter(f -> FileNameUtil.getExtension(f.getFileName().toString()).equals(SCRIPT_EXTENSION))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .skip(offset)
                    .limit(size)
                    .map(n ->
                    {
                        final ServerFunctionInfo info = new ServerFunctionInfo(n);
                        try
                        {
                            info.setLastModified(OffsetDateTime.ofInstant(Files.getLastModifiedTime(n).toInstant(), ZoneOffset.UTC));
                        }
                        catch (IOException e)
                        {
                            logger.warn("Cannot get last modified time of {}: {}", n, e);
                        }
                        return info;
                    })
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String readSourceIfReadable(final Path sourcePath) throws IOException
    {
        if (Files.exists(sourcePath) && Files.isReadable(sourcePath))
        {
            return readSource(sourcePath);
        }
        return null;
    }

    @Override
    public Optional<Path> getApiSpecification()
    {
        final Path specPathYaml = projectConfiguration.getPath().resolve(SPECIFICATION_DIRECTORY).resolve(API_SPECIFICATION_YAML_FILENAME);
        final Path specPathJson = projectConfiguration.getPath().resolve(SPECIFICATION_DIRECTORY).resolve(API_SPECIFICATION_JSON_FILENAME);
        if (Files.exists(specPathYaml))
        {
            return Optional.of(specPathYaml);
        }
        else if (Files.exists(specPathJson))
        {
            return Optional.of(specPathJson);
        }
        return Optional.empty();
    }

    @Override
    public URL getSharedClassPath()
    {
        try
        {
            return sharedPath.toUri().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public List<URL> getLibUrls()
    {
        if (!Files.exists(libPath, LinkOption.NOFOLLOW_LINKS))
        {
            return Collections.emptyList();
        }

        try
        {
            return Files.list(this.libPath).filter(p -> FileNameUtil.getExtension(p.getFileName().toString()).equals(JAR_EXTENSION)).map(IoUtil::toURL).collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
    }

    @Override
    public void close()
    {
        this.watchDir.close();
    }
}