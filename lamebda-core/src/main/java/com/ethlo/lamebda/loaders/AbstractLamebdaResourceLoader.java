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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ApiSpecificationModificationNotice;
import com.ethlo.lamebda.FunctionModificationNotice;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.SourceChangeAware;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.util.Assert;
import groovy.lang.GroovyClassLoader;

public abstract class AbstractLamebdaResourceLoader implements LamebdaResourceLoader, SourceChangeAware
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractLamebdaResourceLoader.class);

    private final FunctionSourcePreProcessor functionSourcePreProcessor;
    private final FunctionPostProcessor functionPostProcessor;
    private Consumer<FunctionModificationNotice> functionChangeListener;
    private Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener;

    public AbstractLamebdaResourceLoader(FunctionSourcePreProcessor functionPreProcesor, FunctionPostProcessor functionPostProcessor)
    {
        this.functionSourcePreProcessor = Assert.notNull(functionPreProcesor, "functionSourcePreProcesor cannot be null");
        this.functionPostProcessor = Assert.notNull(functionPostProcessor, "functionPostProcessor cannot be null");
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
    public ServerFunction load(GroovyClassLoader classLoader, Path sourcePath)
    {
        final Class<ServerFunction> clazz = parseClass(classLoader, sourcePath);
        return functionPostProcessor.process(instantiate(clazz));
    }

    protected ServerFunction instantiate(Class<ServerFunction> clazz)
    {
        try
        {
            return ServerFunction.class.cast(clazz.newInstance());
        }
        catch (InstantiationException | IllegalAccessException exc)
        {
            throw new IllegalStateException("Cannot instantiate class " + clazz.getName(), exc);
        }
    }

    @Override
    public Class<ServerFunction> parseClass(GroovyClassLoader classLoader, Path sourcePath)
    {
        try
        {
            loadLibs(classLoader, sourcePath);

            final String source = readSource(sourcePath);
            final String modifiedSource = functionSourcePreProcessor.process(classLoader, source);

            final Class<?> clazz = classLoader.parseClass(modifiedSource != null ? modifiedSource : source);
            Assert.isTrue(ServerFunction.class.isAssignableFrom(clazz), "Class " + clazz.getName() + " must be instance of class ServerFunction");

            return (Class<ServerFunction>) clazz;
        }
        catch (IOException exc)
        {
            throw new IllegalStateException("Cannot parse class " + sourcePath, exc);
        }
    }

    private void loadLibs(GroovyClassLoader classLoader, final Path sourcePath) throws IOException
    {
        if (! Files.exists(sourcePath))
        {
            return;
        }

        final Path directory = sourcePath.getParent().resolve("lib");

        logger.debug("Using library classpath for script {}: {}", sourcePath, directory);
        if (Files.exists(directory))
        {
            classLoader.addURL(directory.toUri().toURL());
            final File[] files = directory.toFile().listFiles(f -> f.getName().endsWith(SCRIPT_EXTENSION));
            if (files != null)
            {
                for (File file : files)
                {
                    logger.debug("Parsing library class {}", file.getName());
                    try
                    {
                        classLoader.loadClass(file.getName().replaceAll(SCRIPT_EXTENSION, ""));
                    }
                    catch (ClassNotFoundException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    protected void functionChanged(Path sourcePath, ChangeType changeType)
    {
        if (functionChangeListener != null)
        {
            functionChangeListener.accept(new FunctionModificationNotice(changeType, sourcePath));
        }
    }

    protected void apiSpecificationChanged(Path sourcePath, ChangeType changeType)
    {
        if (apiSpecificationChangeListener != null)
        {
            apiSpecificationChangeListener.accept(new ApiSpecificationModificationNotice(changeType, sourcePath));
        }
    }
}
