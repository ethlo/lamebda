package com.ethlo.lamebda.loaders;

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

public class FileSystemChangeAwareResourceLoader extends FileSystemLamebdaResourceLoader implements FileSystemNotificationAware
{
    private Consumer<FunctionModificationNotice> functionChangeListener;
    private Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener;
    private Consumer<FileSystemEvent> libChangeListener;

    private WatchDir watchDir;

    public FileSystemChangeAwareResourceLoader(ProjectConfiguration projectConfiguration, FunctionPostProcessor functionPostProcessor) throws IOException
    {
        super(projectConfiguration, functionPostProcessor);

        if (projectConfiguration.isListenForChanges())
        {
            listenForChanges(projectPath, scriptPath, specificationPath, libPath);
        }
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
    }


    private void libChanged(FileSystemEvent event)
    {
        if (libChangeListener != null)
        {
            libChangeListener.accept(event);
        }
    }

}
