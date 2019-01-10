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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.FileSystemEvent;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class FileSystemLamebdaResourceLoader extends AbstractLamebdaResourceLoader
{
    private static final Logger logger = LoggerFactory.getLogger(FileSystemLamebdaResourceLoader.class);

    public static final String STATIC_DIR = "static";
    public static final String API_SPECIFICATION_YAML = "oas.yaml";
    public static final String API_SPECIFICATION_JSON = "oas.json";
    public static final String SPECIFICATION_DIRECTORY_NAME = "specification";
    private static final String LIB_DIRECTORY_NAME = "lib";

    private final Path projectPath;
    private final Path scriptPath;
    private final Path specificationPath;
    private final Path libPath;

    public FileSystemLamebdaResourceLoader(FunctionSourcePreProcessor functionSourcePreProcessor, FunctionPostProcessor functionPostProcessor, Path projectPath) throws IOException
    {
        super(functionSourcePreProcessor, functionPostProcessor);

        if (!Files.exists(projectPath))
        {
            throw new FileNotFoundException("Cannot use " + projectPath + " as project directory");
        }

        this.projectPath = projectPath;
        this.scriptPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SCRIPT_DIRECTORY_NAME));
        this.specificationPath = IoUtil.ensureDirectoryExists(projectPath.resolve(SPECIFICATION_DIRECTORY_NAME));
        this.libPath = IoUtil.ensureDirectoryExists(projectPath.resolve(LIB_DIRECTORY_NAME));

        logger.info("Project directory: {}", projectPath);
        logger.info("HandlerFunction directory: {}", scriptPath);
        logger.info("Specification directory: {}", specificationPath);
        logger.info("Library path: {}", libPath);

        //listenForChanges(projectPath);
        listenForChanges(scriptPath, specificationPath);
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

        if (filename.endsWith(SCRIPT_EXTENSION) && event.getPath().getParent().equals(scriptPath))
        {
            functionChanged(event.getPath(), changeType);
        }
        else if (filename.equals(API_SPECIFICATION_JSON) || filename.equals(API_SPECIFICATION_YAML))
        {
            apiSpecificationChanged(event.getPath(), changeType);
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
        // TODO: Walk hierarchy!

        final String[] files = projectPath.toFile().list((d, f) -> f.endsWith(SCRIPT_EXTENSION));
        return Arrays.asList(files)
                .stream()
                .skip(offset)
                .limit(size)
                .map(n -> new ServerFunctionInfo(projectPath.resolve(n)))
                .collect(Collectors.toList());
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
    public URL getLibraryClassPath()
    {
        try
        {
            return libPath.toUri().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
}
