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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.FileSystemUtils;

import com.ethlo.lamebda.dao.LocalProjectDao;
import com.ethlo.lamebda.dao.LocalProjectDaoImpl;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.WatchDir;

public class ProjectManager
{
    public static final String WORKDIR_DIRECTORY_NAME = "workdir";
    private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);
    private final Path rootDirectory;
    private final ApplicationContext parentContext;

    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    private final LocalProjectDao localProjectDao;
    private final LamebdaConfiguration rootConfiguration;
    private static final long pid = ProcessHandle.current().pid();
    ;

    private WatchDir watchDir;

    public ProjectManager(final LamebdaConfiguration rootConfiguration, ApplicationContext parentContext) throws IOException
    {
        this.rootConfiguration = rootConfiguration;

        this.rootDirectory = rootConfiguration.getRootDirectory();
        logger.info("Initializing Lamebda with root directory '{}'", rootDirectory.toAbsolutePath());
        logger.debug("Root configuration: {}", rootConfiguration);

        if (!Files.isDirectory(rootDirectory))
        {
            throw new IOException("Specified root directory is not a directory: " + rootDirectory);
        }

        this.parentContext = parentContext;

        this.localProjectDao = new LocalProjectDaoImpl(rootDirectory);

        logger.debug("Parent application context ID: {}", parentContext.getId());

        initializeAll();
    }

    public static Path setupWorkDir(final Path projectPath)
    {
        final Path parentWorkDir = projectPath.resolve(WORKDIR_DIRECTORY_NAME);
        try
        {
            Files.createDirectories(parentWorkDir);
            final Path workDir = Files.createTempDirectory(parentWorkDir, pid + "_");
            try (final Stream<Path> l = Files.list(parentWorkDir))
            {
                l.filter(Files::isDirectory)
                        .filter(path -> !path.getFileName().toString().startsWith(Long.toString(pid)))
                        .forEach(root ->
                        {
                            try
                            {
                                FileSystemUtils.deleteRecursively(root);
                            }
                            catch (IOException exc)
                            {
                                logger.warn("Could not delete temp directory " + workDir + ": " + exc.getMessage());
                            }
                        });
            }
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
            final boolean isProjectJar = projectPath.resolve(projectPath.getFileName().toString() + ".jar").equals(path);
            final boolean isKnownType = isKnownType(path.getFileName().toString());

            if (isWorkDirPath)
            {
                logger.debug("Skipping target file: {}", path);
            }
            else if (e.getChangeType() == ChangeType.DELETED && isProjectPath)
            {
                logger.info("Closing project due to deletion of project directory: {}", e.getPath());
                closeProject(Project.toAlias(e.getPath()));
            }
            else if (e.getChangeType() == ChangeType.MODIFIED && (isKnownType || isProjectPath || isProjectJar))
            {
                logger.info("Reloading project due to modification of {}", path);
                closeProject(Project.toAlias(projectPath));
                loadProject(Project.toAlias(projectPath));
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
        if (rootConfiguration.isDirectoryWatchEnabled())
        {
            setupDirectoryWatcher();
        }
        else
        {
            logger.info("Directory watch is disabled. No automatic reload will occur");
        }

        for (Path projectPath : localProjectDao.getLocalProjectDirectories())
        {
            loadProject(Project.toAlias(projectPath));
        }
    }

    private void closeProject(final String alias)
    {
        final Project existing = projects.remove(alias);
        if (existing != null)
        {
            logger.info("Closing {}", alias);
            existing.close();
        }
    }

    private void loadProject(final String alias)
    {
        logger.info("Loading {}", alias);
        final Path projectPath = rootDirectory.resolve(alias);
        Project project = null;
        try
        {
            final BootstrapConfiguration cfg = new BootstrapConfiguration(rootConfiguration.getRequestPath(), projectPath, System.getProperties());
            project = new ProjectImpl(alias, parentContext, cfg, setupWorkDir(projectPath));
            projects.put(Project.toAlias(projectPath), project);
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

            logger.warn("Unable to load project in {}", projectPath, exc);
        }
    }

    private boolean isKnownType(final String filename)
    {
        return filename.endsWith(ProjectImpl.GROOVY_EXTENSION)
                || filename.endsWith(ProjectImpl.JAVA_EXTENSION)
                || filename.endsWith(ProjectImpl.PROPERTIES_EXTENSION);
    }

    public Map<String, Project> getProjects()
    {
        return this.projects;
    }

    public LamebdaConfiguration getRootConfiguration()
    {
        return rootConfiguration;
    }

    public OffsetDateTime getStartupTime()
    {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(parentContext.getStartupDate()), ZoneId.systemDefault());
    }

    public List<String> getProjectAliases()
    {
        return localProjectDao.getLocalProjectDirectories()
                .stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    public Optional<Project> getByAlias(String alias)
    {
        return projects
                .values()
                .stream()
                .filter(p -> p.getProjectConfiguration().getPath().getFileName().toString().equals(alias))
                .findFirst();
    }

    public void reload(final Project project)
    {
        unload(project);
        load(project);
    }

    public void load(Project project)
    {
        loadProject(project.getAlias());
    }

    public void unload(Project project)
    {
        closeProject(project.getAlias());
    }
}
