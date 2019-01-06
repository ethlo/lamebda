package com.ethlo.lamebda;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.error.ErrorResponse;

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

public class FunctionManagerImpl implements FunctionManager
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionManagerImpl.class);
    private static final String PROPERTIES_EXTENSION = ".properties";

    private Map<String, ServerFunction> functions = new ConcurrentHashMap<>();
    private ClassResourceLoader classResourceLoader;

    public FunctionManagerImpl(ClassResourceLoader classResourceLoader)
    {
        this.classResourceLoader = classResourceLoader;

        if (classResourceLoader instanceof SourceChangeAware)
        {
            ((SourceChangeAware) classResourceLoader).setChangeListener(n -> {
                switch (n.getChangeType())
                {
                    case CREATED:
                    case MODIFIED:
                        try
                        {
                            // Load the function from source
                            final ServerFunction loaded = classResourceLoader.load(n.getSourcePath());

                            internalPostProcess(classResourceLoader, loaded, n.getSourcePath());

                            // Remove the last compilation error if any
                            unload(n.getSourcePath());

                            // Add the function back
                            addFunction(n.getSourcePath(), loaded);
                        }
                        catch (CompilationFailedException exc)
                        {
                            logger.info("Unloading function {} due to script compilation error", n.getSourcePath());
                            unload(n.getSourcePath());
                            throw exc;
                        }
                        break;

                    case DELETED:
                        unload(n.getSourcePath());
                }
            });
        }
    }

    private void internalPostProcess(final ClassResourceLoader classResourceLoader, final ServerFunction func, final String sourcePath)
    {
        if (func instanceof FunctionContextAware)
        {
            ((FunctionContextAware) func).setContext(loadContext(classResourceLoader, func, sourcePath));
        }
    }

    private FunctionContext loadContext(final ClassResourceLoader classResourceLoader, final ServerFunction func, final String sourcePath)
    {
        final FunctionConfiguration config = new FunctionConfiguration();
        final String cfgFilePath = sourcePath.replace(ClassResourceLoader.EXTENSION, PROPERTIES_EXTENSION);
        String cfgContent;
        try
        {
            cfgContent = classResourceLoader.readSourceIfReadable(cfgFilePath);
        }
        catch (IOException exc)
        {
            throw new RuntimeException(exc);
        }

        if (cfgContent != null)
        {
            try
            {
                config.load(new StringReader(cfgContent));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unable to load property file " + cfgFilePath, e);
            }
        }
        return new FunctionContext(config);
    }

    public FunctionManagerImpl addFunction(String filename, ServerFunction func)
    {
        final boolean exists = functions.put(filename, func) != null;
        logger.info(exists ? "'{}' was reloaded" : "'{}' was loaded", filename);
        return this;
    }

    @PostConstruct
    protected void loadAll()
    {
        for (ServerFunctionInfo f : classResourceLoader.findAll(0, Integer.MAX_VALUE))
        {
            try
            {
                addFunction(f.getSourcePath(), classResourceLoader.load(f.getSourcePath()));
            }
            catch (Exception exc)
            {
                logger.error("Error in function {}: {}", f.getSourcePath(), exc.getMessage());
            }
        }
    }

    private void unload(final String sourcePath)
    {
        final ServerFunction func = functions.remove(sourcePath);
        if (func != null)
        {
            logger.info("'{}' was unloaded", sourcePath);
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception
    {
        for (final ServerFunction serverFunction : functions.values())
        {
            if (doHandle(request, response, serverFunction))
            {
                return;
            }
        }

        response.error(ErrorResponse.notFound("No function found to handle '" + request.path() + "'"));
    }

    private boolean doHandle(HttpRequest request, HttpResponse response, ServerFunction f) throws Exception
    {
        try
        {
            final FunctionResult result = f.handle(request, response);
            if (result == null)
            {
                throw new IllegalStateException("A function should never return null. Expected FunctionResult");
            }
            return result == FunctionResult.PROCESSED;
        }
        catch (RuntimeException exc)
        {
            throw handleError(exc);
        }
    }

    private RuntimeException handleError(final RuntimeException exc)
    {
        Throwable cause = exc;
        if (exc instanceof UndeclaredThrowableException && exc.getCause() != null)
        {
            cause = exc.getCause();
        }

        if (cause instanceof RuntimeException)
        {
            throw (RuntimeException) cause;
        }
        throw new RuntimeException(cause);
    }

    public Map<String, ServerFunction> getFunctions()
    {
        return Collections.unmodifiableMap(functions);
    }
}
