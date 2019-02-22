package com.ethlo.lamebda.loaders;

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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

import com.ethlo.lamebda.ApiSpecificationModificationNotice;
import com.ethlo.lamebda.FunctionModificationNotice;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.io.FileSystemEvent;
import com.ethlo.lamebda.io.WatchDir;
import com.ethlo.lamebda.util.FileNameUtil;

public class ChangeAwareFileSystemLamebdaResourceLoader extends FileSystemLamebdaResourceLoader implements FileSystemNotificationAware
{
    private Consumer<FunctionModificationNotice> functionChangeListener;
    private Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener;
    private Consumer<FileSystemEvent> libChangeListener;
    private Consumer<FileSystemEvent> projectConfigChangeListener;

    private WatchDir watchDir;

    public ChangeAwareFileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration, FunctionPostProcessor functionPostProcessor) throws IOException
    {
        super(projectConfiguration, functionPostProcessor);

        if (projectConfiguration.isListenForChanges())
        {
            listenForChanges(projectConfiguration.getPath(), projectConfiguration.getScriptPath(), projectConfiguration.getSpecificationPath(), projectConfiguration.getLibraryPath());
        }

        // Listen for lib folder changes
        setLibChangeListener(n ->
        {
            if (n.getChangeType() == ChangeType.CREATED)
            {
                addClasspath(n.getPath().toAbsolutePath().toString());
            }
        });
    }

    @Override
    public void setFunctionChangeListener(Consumer<FunctionModificationNotice> l)
    {
        this.functionChangeListener = l;
    }

    @Override
    public void setProjectConfigChangeListener(final Consumer<FileSystemEvent> listener)
    {
        this.projectConfigChangeListener = listener;
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

    private void projectConfigChanged(FileSystemEvent event)
    {
        if (projectConfigChangeListener != null)
        {
            projectConfigChangeListener.accept(event);
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

        if (FileNameUtil.getExtension(filename).equals(FileSystemLamebdaResourceLoader.SCRIPT_EXTENSION) && event.getPath().getParent().equals(getProjectConfiguration().getScriptPath()))
        {
            functionChanged(event.getPath(), changeType);
        }
        else if (FileNameUtil.getExtension(filename).equals(JAR_EXTENSION) && event.getPath().getParent().equals(getProjectConfiguration().getLibraryPath()))
        {
            libChanged(event);
        }
        else if (filename.equals(API_SPECIFICATION_JSON_FILENAME) || filename.equals(API_SPECIFICATION_YAML_FILENAME))
        {
            apiSpecificationChanged(event.getPath(), changeType);
        }
        else if (filename.equals(PROJECT_FILENAME) && event.getPath().getParent().equals(getProjectConfiguration().getPath()))
        {
            projectConfigChanged(event);
        }
    }

    private void libChanged(FileSystemEvent event)
    {
        if (libChangeListener != null)
        {
            libChangeListener.accept(event);
        }
    }

}
