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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.WatchDir;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;

public class FunctionManagerDirector
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionManagerDirector.class);

    private final Path rootDirectory;
    private final String rootContext;
    private final ApplicationContext parentContext;

    private Map<Path, FunctionManager> functionManagers = new ConcurrentHashMap<>();
    private WatchDir watchDir;

    public FunctionManagerDirector(final Path rootDirectory, String rootContext, ApplicationContext parentContext) throws IOException
    {
        Assert.notNull(rootDirectory, "rootDirectory cannot be null");
        Assert.notNull(rootDirectory, "rootContext cannot be null");

        logger.info("Initializing Lamebda");

        if (!Files.isDirectory(rootDirectory))
        {
            throw new IOException("Specified root directory is not a directory: " + rootDirectory);
        }

        this.rootDirectory = rootDirectory;
        this.rootContext = rootContext;
        this.parentContext = parentContext;

        initializeAll();
    }

    private void setupDirectoryWatcher() throws IOException
    {
        // Register directory watcher to discover new project directories created in root directory
        this.watchDir = new WatchDir(e ->
        {
            final Path path = e.getPath();
            final Path projectPath = getProjectPath(path);
            final boolean isTargetPath = path.toAbsolutePath().startsWith(projectPath.resolve("target").toAbsolutePath());
            final boolean isProjectPath = projectPath.equals(path);
            final boolean isKnownType = isKnownType(path.getFileName().toString());

            if (isTargetPath)
            {
                logger.debug("Skipping target file: {}", path);
            }
            else if (e.getChangeType() == ChangeType.DELETED && isProjectPath)
            {
                logger.info("Closing project due to deletion of project directory: {}", e.getPath());
                close(projectPath);
            }
            else if (e.getChangeType() == ChangeType.MODIFIED && (isKnownType || isProjectPath))
            {
                logger.info("Reloading project due to modification of {}", path);
                close(projectPath);
                create(projectPath);
            }
            else if (e.getChangeType() == ChangeType.DELETED && isKnownType)
            {
                logger.info("Reloading project due to deletion of {}", path);
                close(projectPath);
                create(projectPath);
            }
        }, true, rootDirectory);
        new Thread()
        {
            @Override
            public void run()
            {
                setName("watch-dir");
                logger.info("Watching {} for changes", rootDirectory);
                watchDir.processEvents();
            }
        }.start();
    }

    private Path getProjectPath(final Path path)
    {
        Path latest = path;
        do
        {
            if (rootDirectory.equals(latest.getParent()))
            {
                return latest;
            }
            latest = latest.getParent();
        } while (latest != null);

        throw new IllegalStateException("Could not determine project path");
    }

    private void initializeAll() throws IOException
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

    private void create(final Path projectPath)
    {
        logger.info("Loading {}", projectPath);
        FileSystemLamebdaResourceLoader lamebdaResourceLoader = null;
        FunctionManagerImpl fm = null;
        try
        {
            lamebdaResourceLoader = createResourceLoader(projectPath);
            fm = new FunctionManagerImpl(parentContext, lamebdaResourceLoader);
            functionManagers.put(projectPath, fm);
        }
        catch (Exception exc)
        {
            try
            {
                if (lamebdaResourceLoader != null)
                {
                    lamebdaResourceLoader.close();
                }

                if (fm != null)
                {
                    fm.close();
                }
            }
            catch (Exception e)
            {
                logger.warn("An error occurred cleaning up failed project initialization", e);
            }

            logger.warn("Unable to load project in " + projectPath, exc);
        }
    }

    private boolean isKnownType(final String filename)
    {
        return filename.endsWith(FileSystemLamebdaResourceLoader.GROOVY_EXTENSION)
                || filename.endsWith(FileSystemLamebdaResourceLoader.JAVA_EXTENSION)
                || filename.endsWith(FileSystemLamebdaResourceLoader.PROPERTIES_EXTENSION)
                || filename.equals(FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);
    }

    private FileSystemLamebdaResourceLoader createResourceLoader(Path projectPath)
    {
        final ProjectConfiguration cfg = ProjectConfiguration.builder(rootContext, projectPath).loadIfExists().build();
        try
        {
            return new FileSystemLamebdaResourceLoader(cfg);
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
