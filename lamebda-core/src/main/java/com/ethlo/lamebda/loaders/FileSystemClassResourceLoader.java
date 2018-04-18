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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.ethlo.lamebda.ChangeType;
import com.ethlo.lamebda.ClassResourceLoader;
import com.ethlo.lamebda.HandlerFunctionInfo;

public class FileSystemClassResourceLoader extends ClassResourceLoader
{
    private static final Logger logger = LoggerFactory.getLogger(FileSystemClassResourceLoader.class);
    
    private final String basePath;
    private final WatchService watchService;

    public FileSystemClassResourceLoader(ApplicationContext applicationContext, String basePath) throws IOException
    {
        super(applicationContext);
        this.basePath = basePath;
        this.watchService = FileSystems.getDefault().newWatchService();
        final Path path = Paths.get(basePath);
        if (! path.toFile().exists())
        {
            throw new FileNotFoundException("Cannot use " + path.toAbsolutePath() + " as source directory for functions, as it does not exist");
        }
        logger.info("Using directory {} as source directory for handler functions", path.toAbsolutePath());
        listenForChanges(path);
    }

    private Modifier getComSunNioFileSensitivityWatchEventModifierHigh()
    {
        try
        {
            final Class<?> c = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier");
            final Field f = c.getField("HIGH");
            return (Modifier) f.get(c);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private WatchKey register(Path dir) throws IOException
    {
        final Modifier high = getComSunNioFileSensitivityWatchEventModifierHigh();
        return (high == null) ? dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY) : dir.register(watchService, new WatchEvent.Kind<?>[]
        { StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY }, high);
    }

    private void listenForChanges(Path path) throws IOException
    {
        register(path);
        
        new Thread()
        {
            @Override
            public void run()
            {
                setName("function-watcher");
                try
                {
                    WatchKey key;
                    while ((key = watchService.take()) != null)
                    {
                        for (WatchEvent<?> event : key.pollEvents())
                        {
                            final String fileName = Path.class.cast(event.context()).toString();
                            doLoad(event, fileName);
                        }
                        key.reset();
                    }
                }
                catch (InterruptedException exc)
                {
                    logger.info("Stopping function watch service");
                    Thread.currentThread().interrupt();
                }
            }

            private void doLoad(WatchEvent<?> event, final String fileName)
            {
                try
                {
                    fileChanged(Paths.get(basePath, fileName), event.kind());
                }
                catch (Exception exc)
                {
                    logger.warn("Unable to reload {}: {}", fileName, exc.getMessage(), exc);
                }
            }
        }.start();
    }
    
    private String getBaseName(String f)
    {
        final int idx = f.lastIndexOf('.');
        return idx > 0 ? f.substring(0, idx) : f;
    }

    private void fileChanged(Path path, Kind<?> k)
    {
        final ChangeType changeType = ChangeType.from(k);
        logger.debug("Notifying due to {} changed: {}", path, changeType);
        functionChanged(getBaseName(path.getFileName().toString()), changeType);
    }

    @Override
    public String load(String name) throws IOException
    {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(Paths.get(basePath, name).toFile()), StandardCharsets.UTF_8)))
        {
            return r.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    @Override
    public Page<HandlerFunctionInfo> findAll(Pageable pageable)
    {
        final String[] files = Paths.get(basePath).toFile().list((d,f)->f.endsWith(".groovy"));
        final List<HandlerFunctionInfo> content = Arrays.asList(files)
            .stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .map(n->new HandlerFunctionInfo(getBaseName(n)))
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, files.length);
    }
}
