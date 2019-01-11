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
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ApiSpecificationModificationNotice;
import com.ethlo.lamebda.FunctionModificationNotice;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.SourceChangeAware;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.FileSystemEvent;
import com.ethlo.lamebda.io.WatchDir;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.FileNameUtil;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class FileSystemLamebdaResourceLoader implements LamebdaResourceLoader, SourceChangeAware
{
    private static final Logger logger = LoggerFactory.getLogger(FileSystemLamebdaResourceLoader.class);

    public static final String API_SPECIFICATION_YAML = "oas.yaml";
    public static final String API_SPECIFICATION_JSON = "oas.json";

    public static final String SCRIPT_EXTENSION = "groovy";
    private static final String JAR_EXTENSION = "jar";

    public static final String SCRIPT_DIRECTORY_NAME = "scripts";
    public static final String STATIC_DIR = "static";
    public static final String SPECIFICATION_DIRECTORY_NAME = "specification";
    public static final String SHARED_DIRECTORY_NAME = "shared";
    public static final String LIB_DIRECTORY_NAME = "lib";


    private final Path projectPath;
    private final Path scriptPath;
    private final Path specificationPath;
    private final Path sharedPath;
    private final Path libPath;

    private final FunctionSourcePreProcessor functionSourcePreProcessor;
    private final FunctionPostProcessor functionPostProcessor;

    private Consumer<FunctionModificationNotice> functionChangeListener;
    private Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener;
    private Consumer<FileSystemEvent> libChangeListener;

    public FileSystemLamebdaResourceLoader(FunctionSourcePreProcessor functionSourcePreProcessor, FunctionPostProcessor functionPostProcessor, Path projectPath) throws IOException
    {
        this.functionSourcePreProcessor = Assert.notNull(functionSourcePreProcessor, "functionSourcePreProcesor cannot be null");
        this.functionPostProcessor = Assert.notNull(functionPostProcessor, "functionPostProcessor cannot be null");

        if (!Files.exists(projectPath))
        {
            throw new FileNotFoundException("Cannot use " + projectPath + " as project directory");
        }

        this.projectPath = projectPath;
        this.scriptPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SCRIPT_DIRECTORY_NAME));
        this.specificationPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SPECIFICATION_DIRECTORY_NAME));
        this.sharedPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SHARED_DIRECTORY_NAME));
        this.libPath = IoUtil.ensureDirectoryExists(projectPath.resolve(LIB_DIRECTORY_NAME));

        logger.info("Project directory: {}", projectPath);
        logger.info("HandlerFunction directory: {}", scriptPath);
        logger.info("Specification directory: {}", specificationPath);
        logger.info("Shared path: {}", sharedPath);
        logger.info("Library path: {}", libPath);

        listenForChanges(scriptPath, specificationPath);
    }

    @Override
    public void setFunctionChangeListener(Consumer<FunctionModificationNotice> l)
    {
        this.functionChangeListener = l;
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
        return functionPostProcessor.process(instantiate(clazz));
    }

    protected ServerFunction instantiate(Class<ServerFunction> clazz)
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

            return (Class<ServerFunction>) clazz;
        }
        catch (IOException exc)
        {
            throw new IllegalStateException("Cannot parse class " + sourcePath, exc);
        }
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
        final WatchDir watchDir = new WatchDir(e -> {
            logger.debug("{}", e);

            try
            {
                fileChanged(e);
            }
            catch (Exception exc)
            {
                logger.warn("Error during file changed event processing: {}", exc.getMessage(), exc);
            }
        }, paths);

        new Thread()
        {
            @Override
            public void run()
            {
                setName("function-watcher");
                logger.info("Watching {} recursively", Arrays.asList(paths));
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
            libChanged(event.getPath(), changeType);
        }
        else if (filename.equals(API_SPECIFICATION_JSON) || filename.equals(API_SPECIFICATION_YAML))
        {
            apiSpecificationChanged(event.getPath(), changeType);
        }
    }

    private void libChanged(final Path path, final ChangeType changeType)
    {
        if (libChangeListener != null)
        {
            libChangeListener.accept(new FunctionModificationNotice(changeType, path));
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
                    .skip(offset)
                    .limit(size)
                    .map(n -> new ServerFunctionInfo(n))
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
        final Path specPathYaml = projectPath.resolve(SPECIFICATION_DIRECTORY_NAME).resolve(API_SPECIFICATION_YAML);
        final Path specPathJson = projectPath.resolve(SPECIFICATION_DIRECTORY_NAME).resolve(API_SPECIFICATION_JSON);
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
}
