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
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import com.google.common.base.CaseFormat;

import groovy.lang.GroovyClassLoader;

public abstract class ClassResourceLoader
{
    private static final Logger logger = LoggerFactory.getLogger(ClassResourceLoader.class);
    private final ApplicationContext applicationContext;
    private Consumer<FunctionModificationNotice> changeListener;
    
    public ClassResourceLoader(ApplicationContext applicationContext)
    {
        Assert.notNull(applicationContext, "applicationContext cannot be null");
        this.applicationContext = applicationContext;
    }
    
    /**
     * Load the contents of the named class
     * @param name The class name
     * @return The contents of the specified name
     * @throws IOException If the class could not be found or loaded
     */
    public abstract String load(String name) throws IOException;
    
    /**
     * Return a list of all known functions
     * @param pageable
     * @return 
     */
    public abstract Page<HandlerFunctionInfo> findAll(Pageable pageable);
    
    /**
     * Set a listener that gets notified whenever the function's source changes
     * @param l The listener
     */
    public void setChangeListener(Consumer<FunctionModificationNotice> l)
    {
        this.changeListener = l;
    }
    
    public final ServerFunction loadClass(String name)
    {
        try (final GroovyClassLoader classLoader = new GroovyClassLoader())
        {
            final Class<?> clazz = classLoader.parseClass(load(name + ".groovy"));
            Assert.isTrue(name.equals(clazz.getSimpleName()), "Incorrect class name " + clazz.getName() + " in " + name);
            final Object instance = clazz.newInstance();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(instance);
            return ServerFunction.class.cast(instance);
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

    public String loadApiSpec(String functionName)
    {
        final String fileName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, functionName) + ".json";
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
