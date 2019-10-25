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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

import com.ethlo.lamebda.dao.LocalProjectDao;
import com.ethlo.lamebda.dao.LocalProjectDaoImpl;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.WatchDir;
import com.ethlo.lamebda.loader.http.HttpArtifactLoader;
import com.ethlo.lamebda.util.IoUtil;

public class ProjectManager
{
    public static final String WORKDIR_DIRECTORY_NAME = "workdir";
    private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);
    private final Path rootDirectory;
    private final String rootContext;
    private final ApplicationContext parentContext;

    private final Map<Path, Project> projects = new ConcurrentHashMap<>();
    private final HttpArtifactLoader artifactLoader;
    private final LocalProjectDao localProjectDao;

    private WatchDir watchDir;

    public ProjectManager(final Path rootDirectory, String rootContext, ApplicationContext parentContext) throws IOException
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

        this.localProjectDao = new LocalProjectDaoImpl(rootDirectory);
        this.artifactLoader = new HttpArtifactLoader();

        initializeAll();
    }

    public static Path setupWorkDir(final Path projectPath)
    {
        final Path parentWorkDir = projectPath.resolve(WORKDIR_DIRECTORY_NAME);
        try
        {
            Files.createDirectories(parentWorkDir);
            final Path workDir = Files.createTempDirectory(parentWorkDir, null);

            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                try
                {
                    FileSystemUtils.deleteRecursively(workDir);
                    if (IoUtil.isEmptyDir(workDir.getParent()))
                    {
                        IoUtil.deleteDirectory(workDir.getParent());
                    }
                }
                catch (IOException ex)
                {
                    logger.debug("Could not delete temp directory " + workDir + ": " + ex.getMessage());
                }
            }));

            return workDir;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void setupDirectoryWatcher() throws IOException
    {
        // Register directory watcher to discover new project directories created in root directory
        this.watchDir = new WatchDir(e ->
        {
            final Path path = e.getPath();
            final Path projectPath = getProjectPath(path);
            final boolean isWorkDirPath = path.toAbsolutePath().startsWith(projectPath.resolve(WORKDIR_DIRECTORY_NAME).toAbsolutePath());
            final boolean isProjectPath = projectPath.equals(path);
            final boolean isKnownType = isKnownType(path.getFileName().toString());

            if (isWorkDirPath)
            {
                logger.debug("Skipping target file: {}", path);
            }
            else if (e.getChangeType() == ChangeType.DELETED && isProjectPath)
            {
                logger.info("Closing project due to deletion of project directory: {}", e.getPath());
                closeProject(projectPath);
            }
            else if (e.getChangeType() == ChangeType.MODIFIED && (isKnownType || isProjectPath))
            {
                logger.info("Reloading project due to modification of {}", path);
                closeProject(projectPath);
                loadProject(projectPath);
            }
        }, true, rootDirectory);
        new Thread()
        {
            @Override
            public void run()
            {
                setName("watch-dir");
                logger.debug("Watching {} for changes", rootDirectory);
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

        for (Path projectPath : localProjectDao.getLocalProjectDirectories())
        {
            loadProject(projectPath);
        }
    }

    private void closeProject(final Path projectPath)
    {
        final Project existing = this.projects.remove(projectPath);
        if (existing != null)
        {
            logger.info("Closing {}", projectPath);
            existing.close();
        }
    }

    private void loadProject(final Path projectPath)
    {
        logger.info("Loading {}", projectPath);
        Project project = null;
        try
        {
            artifactLoader.prepareArtifact(projectPath);

            final BootstrapConfiguration cfg = new BootstrapConfiguration(rootContext, projectPath, System.getProperties());

            project = new ProjectImpl(parentContext, cfg, setupWorkDir(projectPath));
            projects.put(projectPath, project);
        }
        catch (Exception exc)
        {
            try
            {
                if (project != null)
                {
                    project.close();
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
        return filename.endsWith(ProjectImpl.GROOVY_EXTENSION)
                || filename.endsWith(ProjectImpl.JAVA_EXTENSION)
                || filename.endsWith(ProjectImpl.PROPERTIES_EXTENSION)
                || filename.equals(ProjectImpl.API_SPECIFICATION_YAML_FILENAME);
    }

    public Map<Path, Project> getProjects()
    {
        return this.projects;
    }
}
