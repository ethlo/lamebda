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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.WatchDir;
import com.ethlo.lamebda.loaders.ChangeAwareFileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.FunctionPostProcessor;

public class FunctionManagerDirector
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionManagerDirector.class);

    private final Path rootDirectory;
    private final String rootContext;

    private FunctionPostProcessor functionPostProcessor;

    private Map<Path, FunctionManager> functionManagers = new ConcurrentHashMap<>();
    private WatchDir watchDir;

    public FunctionManagerDirector(final Path rootDirectory, String rootContext, FunctionPostProcessor functionPostProcessor) throws IOException
    {
        logger.info("Initializing Lamebda");

        if (!Files.isDirectory(rootDirectory))
        {
            throw new IOException("Specified root directory is not a directory: " + rootDirectory);
        }

        this.rootDirectory = rootDirectory;
        this.rootContext = rootContext;
        this.functionPostProcessor = functionPostProcessor;

        initializeAll();
    }

    private void setupDirectoryWatcher() throws IOException
    {
        // Register directory watcher to discover new project directories created in root directory
        this.watchDir = new WatchDir(e -> {
            if (isValidProjectDir(e.getPath()))
            {
                if (e.getChangeType() == ChangeType.DELETED)
                {
                    logger.info("Closing project due to deletion: {}", e.getPath());
                    close(e.getPath());
                }

                if (e.getChangeType() == ChangeType.MODIFIED)
                {
                    logger.info("Loading project due to modification {}", e.getPath());
                    close(e.getPath());
                    create(e.getPath());
                }
            }
        }, false, rootDirectory);
        new Thread()
        {
            @Override
            public void run()
            {
                setName("root-filesystem-watcher");
                logger.info("Watching {} for changes", Arrays.asList(rootDirectory));
                watchDir.processEvents();
            }
        }.start();
    }

    public void initializeAll() throws IOException
    {
        setupDirectoryWatcher();

        final List<Path> directories;
        try
        {
            directories = Files.list(rootDirectory)
                    .filter(this::isValidProjectDir)
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        for (Path projectPath : directories)
        {
            close(projectPath);
            create(projectPath);
        }
    }

    private boolean isValidProjectDir(final Path p)
    {
        try
        {
            final boolean validDirectory = !Files.isHidden(p) && Files.isDirectory(p) && p.getParent().equals(rootDirectory);
            final boolean validCompressedFile = !Files.isHidden(p) && Files.isRegularFile(p) && p.getParent().equals(rootDirectory) && (p.toString().endsWith(".zip") || p.toString().endsWith(".jar"));
            return validDirectory || validCompressedFile;
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    private void close(final Path projectPath)
    {
        final FunctionManager existing = this.functionManagers.remove(projectPath);
        if (existing != null)
        {
            logger.info("Closing {}", projectPath);
            existing.close();
        }
    }

    private FunctionManager create(final Path projectPath)
    {
        logger.info("Loading {}", projectPath);
        final FileSystemLamebdaResourceLoader lamebdaResourceLoader = createResourceLoader(projectPath);
        final FunctionManager fm = new FunctionManagerImpl(lamebdaResourceLoader);
        functionManagers.put(projectPath, fm);
        return fm;
    }

    private FileSystemLamebdaResourceLoader createResourceLoader(Path projectPath)
    {
        final ProjectConfiguration cfg = ProjectConfiguration.builder(rootContext, projectPath).loadIfExists().build();
        try
        {
            return new ChangeAwareFileSystemLamebdaResourceLoader(cfg, functionPostProcessor);
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    public Map<Path, FunctionManager> getFunctionManagers()
    {
        return this.functionManagers;
    }
}
