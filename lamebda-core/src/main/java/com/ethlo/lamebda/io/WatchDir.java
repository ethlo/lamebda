package com.ethlo.lamebda.io;

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

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchDir implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(WatchDir.class);

    private static WatchEvent.Modifier modifier = getComSunNioFileSensitivityWatchEventModifierHigh();

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final Consumer<FileSystemEvent> listener;
    private final boolean recursive;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }

    public void registerRecursively(Path dir) throws IOException
    {
        final WatchKey key = register(dir);
        keys.put(key, dir);
    }

    private static WatchEvent.Modifier getComSunNioFileSensitivityWatchEventModifierHigh()
    {
        try
        {
            final Class<?> c = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier");
            final Field f = c.getField("HIGH");
            return (WatchEvent.Modifier) f.get(c);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private WatchKey register(Path dir) throws IOException
    {
        if (modifier == null)
        {
            return dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        }
        else
        {
            return dir.register(watcher, new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY}, modifier);
        }
    }

    public WatchDir(Consumer<FileSystemEvent> listener, boolean recursive, Path... dirs) throws IOException
    {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.listener = listener;
        this.recursive = recursive;
        for (Path dir : dirs)
        {
            registerDir(dir);
        }
    }

    private void registerDir(final Path dir) throws IOException
    {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (recursive)
                {
                    registerRecursively(dir);
                    return FileVisitResult.CONTINUE;
                }
                else
                {
                    register(dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        });
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents()
    {
        for (; ; )
        {
            // wait for key to be signalled
            WatchKey key;
            try
            {
                key = watcher.take();
            }
            catch (InterruptedException x)
            {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null)
            {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents())
            {
                WatchEvent.Kind kind = event.kind();
                if (kind == OVERFLOW)
                {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                final WatchEvent<Path> ev = cast(event);
                final Path name = ev.context();
                final Path child = dir.resolve(name);

                final FileSystemEvent e = new FileSystemEvent(ChangeType.from(event.kind()), child);
                logger.debug("File system changed: {}", e);
                listener.accept(e);

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if ((kind == ENTRY_CREATE))
                {
                    try
                    {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS))
                        {
                            logger.debug("Watching new directory {}", child);
                            registerDir(child);
                        }
                    }
                    catch (IOException exc)
                    {
                        logger.warn("Problem registering {}", child);
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid)
            {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty())
                {
                    break;
                }
            }
        }
    }

    @Override
    public void close()
    {
        keys.forEach((key, path) -> key.cancel());
    }
}
