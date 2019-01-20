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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.FunctionPostProcessor;
import com.ethlo.lamebda.loaders.FunctionSourcePreProcessor;

public class FunctionManagerDirector
{
    private final Path rootDirectory;
    private final String rootContext;

    private FunctionSourcePreProcessor preNotification = (c,s)->s;
    private FunctionPostProcessor functionPostProcessor;

    private Map<Path, FunctionManager> functionManagers = new ConcurrentHashMap<>();

    public FunctionManagerDirector(final Path rootDirectory, String rootContext, FunctionPostProcessor functionPostProcessor)
    {
        this.rootDirectory = rootDirectory;
        this.rootContext = rootContext;
        this.functionPostProcessor = functionPostProcessor;

        initialize();
    }

    public void initialize()
    {
        final List<Path> directories;
        try
        {
            directories = Files.list(rootDirectory)
                    .filter(p ->
                    {
                        try
                        {
                            return !Files.isHidden(p) && Files.isDirectory(p);
                        }
                        catch (IOException exc)
                        {
                            throw new UncheckedIOException(exc);
                        }
                    })
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        for (Path projectPath : directories)
        {
            functionManagers.put(projectPath, initProject(projectPath));
        }
    }


    private FunctionManager initProject(Path projectPath)
    {
        try
        {
            final ProjectConfiguration cfg = ProjectConfiguration.builder(rootContext, projectPath).loadIfExists().build();
            final FileSystemLamebdaResourceLoader lamebdaResourceLoader = new FileSystemLamebdaResourceLoader(cfg, preNotification, functionPostProcessor);
            lamebdaResourceLoader.setProjectChangeListener(n -> {
                final FunctionManager existing = this.functionManagers.remove(projectPath);
                existing.close();
                initProject(projectPath);
                this.functionManagers.put(projectPath, initProject(projectPath));
            });
            final FunctionManager fm = new FunctionManagerImpl(lamebdaResourceLoader);
            return fm;

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
