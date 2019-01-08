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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.FileSystemEvent;

public class FileSystemLamebdaResourceLoader extends AbstractLamebdaResourceLoader
{
    private static final Logger logger = LoggerFactory.getLogger(FileSystemLamebdaResourceLoader.class);

    public static final String STATIC_DIR = "static";
    public static final String API_SPECIFICATION_YAML = "oas.yaml";
    public static final String API_SPECIFICATION_JSON = "oas.json";
    public static final String SPECIFICATION_DIR = "specification";

    private final Path basePath;
    private final WatchService watchService;

    public FileSystemLamebdaResourceLoader(FunctionSourcePreProcessor functionSourcePreProcessor, FunctionPostProcessor functionPostProcessor, Path basePath) throws IOException
    {
        super(functionSourcePreProcessor, functionPostProcessor);
        this.basePath = basePath;
        this.watchService = FileSystems.getDefault().newWatchService();
        if (!Files.exists(basePath))
        {
            throw new FileNotFoundException("Cannot use " + basePath + " as source directory for functions, as it does not exist");
        }
        logger.info("Using directory {} as source directory for handler functions", basePath);
        listenForChanges(basePath);
    }

    private void listenForChanges(Path path) throws IOException
    {
        final WatchDir watchDir = new WatchDir(path, e -> {
            logger.debug("{}", e);

            try
            {
                fileChanged(e);
            }
            catch (Exception exc)
            {
                logger.warn("Error during file changed event processing: {}", exc.getMessage(), exc);
            }
        });

        new Thread()
        {
            @Override
            public void run()
            {
                setName("function-watcher");
                logger.info("Watching {} recursively", path);
                watchDir.processEvents();
            }
        }.start();
    }

    private void fileChanged(FileSystemEvent event)
    {
        final String filename = event.getPath().getFileName().toString();
        final ChangeType changeType = event.getChangeType();

        if (filename.endsWith(SCRIPT_EXTENSION) && event.getPath().getParent().equals(basePath))
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

        final String[] files = basePath.toFile().list((d, f) -> f.endsWith(SCRIPT_EXTENSION));
        return Arrays.asList(files)
                .stream()
                .skip(offset)
                .limit(size)
                .map(n -> new ServerFunctionInfo(basePath.resolve(n)))
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
        final Path specPathYaml = basePath.resolve(SPECIFICATION_DIR).resolve(API_SPECIFICATION_YAML);
        final Path specPathJson = basePath.resolve(SPECIFICATION_DIR).resolve(API_SPECIFICATION_JSON);
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
}
