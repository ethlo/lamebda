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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ChangeType;
import com.ethlo.lamebda.ClassResourceLoader;
import com.ethlo.lamebda.FunctionModificationNotice;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.SourceChangeAware;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.FileNameUtil;
import groovy.lang.GroovyClassLoader;

public abstract class AbstractClassResourceLoader implements ClassResourceLoader, SourceChangeAware
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractClassResourceLoader.class);

    private final FunctionLoadPreNotification functionLoadPreNotification;
    private final FunctionPostProcessor functionPostProcessor;
    private Consumer<FunctionModificationNotice> changeListener;

    public AbstractClassResourceLoader(FunctionLoadPreNotification functionPreProcesor, FunctionPostProcessor functionPostProcessor)
    {
        this.functionLoadPreNotification = Assert.notNull(functionPreProcesor, "functionPreProcesor cannot be null");
        this.functionPostProcessor = Assert.notNull(functionPostProcessor, "functionPostProcessor cannot be null");
    }

    @Override
    public void setChangeListener(Consumer<FunctionModificationNotice> l)
    {
        this.changeListener = l;
    }

    @Override
    public ServerFunction load(String sourcePath)
    {
        final Class<ServerFunction> clazz = parseClass(sourcePath);
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
    public Class<ServerFunction> parseClass(String sourcePath)
    {
        try (final GroovyClassLoader classLoader = new GroovyClassLoader())
        {
            functionLoadPreNotification.process(classLoader, sourcePath);

            loadLibs(classLoader, sourcePath);

            final Class<?> clazz = classLoader.parseClass(readSource(sourcePath));
            Assert.isTrue(ServerFunction.class.isAssignableFrom(clazz), "Class " + clazz.getName() + " must be instance of class ServerFunction");

            return (Class<ServerFunction>) clazz;
        }
        catch (IOException exc)
        {
            throw new IllegalStateException("Cannot parse class " + sourcePath, exc);
        }
    }

    private void loadLibs(GroovyClassLoader classLoader, final String sourcePath) throws IOException
    {
        final File directory = new File(FileNameUtil.getDirectory(sourcePath), "lib");
        logger.debug("Using library classpath for script {}: {}", sourcePath, directory);
        if (directory.exists())
        {
            classLoader.addURL(directory.toURI().toURL());
            final File[] files = directory.listFiles(f -> f.getName().endsWith(EXTENSION));
            for (File file : files)
            {
                logger.debug("Parsing library class {}", file.getName());
                try
                {
                    classLoader.loadClass(file.getName().replaceAll(".groovy", ""));
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected void functionChanged(String sourcePath, ChangeType changeType)
    {
        changeListener.accept(new FunctionModificationNotice(sourcePath, changeType));
    }
}
