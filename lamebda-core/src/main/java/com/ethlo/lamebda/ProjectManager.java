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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

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
    private WatchDir watchDir;

    public ProjectManager(final LamebdaConfiguration lamebdaConfiguration, ApplicationContext parentContext) throws IOException
    {
        this.rootConfiguration = lamebdaConfiguration;

        this.rootDirectory = lamebdaConfiguration.getRootDirectory();
        logger.info("Initializing Lamebda with configuration:\n{}", lamebdaConfiguration.toPrettyString());

        if (!Files.isDirectory(rootDirectory))
        {
            throw new IOException("Specified root directory is not a directory: " + rootDirectory);
        }

        this.parentContext = parentContext;

        this.localProjectDao = new LocalProjectDaoImpl(rootDirectory);

        logger.debug("Parent application context ID: {}", parentContext.getId());

        initializeAll();
    }

    private void setupDirectoryWatcher() throws IOException
    {
        // Register directory watcher to discover new project directories created in root directory
        this.watchDir = new WatchDir(e ->
        {
            final Path path = e.path();
            final Path projectPath = getProjectPath(path);
            final String alias = Project.toAlias(projectPath);
            final boolean isWorkDirPath = path.toAbsolutePath().startsWith(projectPath.resolve(WORKDIR_DIRECTORY_NAME).toAbsolutePath());
            final boolean isProjectPath = projectPath.equals(path);
            final boolean isProjectJar = projectPath.resolve(projectPath.getFileName().toString() + ".jar").equals(path);
            final boolean isKnownType = isKnownType(path.getFileName().toString());
            final Path noReloadFile = projectPath.resolve(".no_reload");
            final boolean reloadDisabledByFile = Files.exists(noReloadFile);

            if (isWorkDirPath)
            {
                logger.debug("Skipping target file: {}", path);
            }
            else if (e.changeType() == ChangeType.DELETED && isProjectPath)
            {
                logger.info("Closing project due to deletion of project directory: {}", e.path());
                closeProject(alias);
            }
            else if (e.changeType() == ChangeType.MODIFIED && (isKnownType || isProjectPath || isProjectJar))
            {
                if (!reloadDisabledByFile)
                {
                    logger.info("Reloading project {} due to modification of {}", alias, path);
                    closeProject(alias);
                    loadProject(alias);
                }
                else
                {
                    logger.info("Reload is temporarily disabled for project {} due to the presence of file {}", alias, noReloadFile);
                }
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

        Project project = null;
        try
        {
            final BootstrapConfiguration cfg = new BootstrapConfiguration(rootConfiguration.getRequestPath(), rootDirectory.resolve(alias), System.getProperties());
            project = new ProjectImpl(alias, parentContext, cfg);
            projects.put(alias, project);
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

            if (rootConfiguration.haltOnError())
            {
                throw new ProjectLoadException("Unable to load project " + alias, exc);
            }
            else
            {
                logger.warn("Unable to load project {}", alias, exc);
            }
        }
    }

    private boolean isKnownType(final String filename)
    {
        return filename.endsWith(ProjectImpl.PROPERTIES_EXTENSION);
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

    public void load(Project project)
    {
        loadProject(project.getAlias());
    }

    public void unload(Project project)
    {
        closeProject(project.getAlias());
    }
}
