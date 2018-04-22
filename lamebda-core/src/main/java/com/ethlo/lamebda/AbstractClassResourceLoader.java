package com.ethlo.lamebda;

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

import java.io.IOException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.loaders.FunctionPostProcesor;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.StringUtil;

import groovy.lang.GroovyClassLoader;

public abstract class AbstractClassResourceLoader implements ClassResourceLoader
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractClassResourceLoader.class);
    private final FunctionPostProcesor functionPostProcesor;
    private Consumer<FunctionModificationNotice> changeListener;
    
    public AbstractClassResourceLoader()
    {
        this.functionPostProcesor = f->f;
    }
    
    public AbstractClassResourceLoader(FunctionPostProcesor functionPostProcesor)
    {
        this.functionPostProcesor = Assert.notNull(functionPostProcesor, "functionPostProcesor cannot be null");
    }
    
    @Override
    public void setChangeListener(Consumer<FunctionModificationNotice> l)
    {
        this.changeListener = l;
    }
    
    @Override
    public final ServerFunction loadClass(String name)
    {
        try (final GroovyClassLoader classLoader = new GroovyClassLoader())
        {
            final Class<?> clazz = classLoader.parseClass(load(name + ".groovy"));
            Assert.isTrue(name.equals(clazz.getSimpleName()), "Incorrect class name " + clazz.getName() + " in " + name);
            return functionPostProcesor.process(ServerFunction.class.cast(clazz.newInstance()));
        }
        catch (InstantiationException | IllegalAccessException | IOException exc)
        {
            throw new IllegalStateException("Cannot load class " + name, exc);
        }
    }

    protected void functionChanged(String name, ChangeType changeType)
    {
        changeListener.accept(new FunctionModificationNotice(name, changeType));
    }

    @Override
    public String loadApiSpec(String functionName)
    {
        final String fileName = StringUtil.hyphenToCamelCase(functionName) + ".json";
        try
        {
            return load(fileName);
        }
        catch (IOException exc)
        {
            logger.warn("No API documentation file {} found for function {}", fileName, functionName);
            return null;
        }
    }
}
