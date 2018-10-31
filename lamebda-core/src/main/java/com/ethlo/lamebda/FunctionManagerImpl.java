package com.ethlo.lamebda;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.functions.ApiDocFunction;
import com.ethlo.lamebda.functions.ApiSpecFunction;
import com.ethlo.lamebda.functions.LastCompilationErrorFunction;

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

    private Map<String, ServerFunction> functions = new ConcurrentHashMap<>();
    private ClassResourceLoader loader;
    private FunctionManagerConfig config;

    public FunctionManagerImpl(ClassResourceLoader loader, final FunctionManagerConfig functionManagerConfig)
    {
        this.loader = loader;
        this.config = functionManagerConfig;
        loader.setChangeListener(n -> {
            switch (n.getChangeType())
            {
                case CREATED:
                case MODIFIED:
                    try
                    {
                        // Load the function from source
                        final ServerFunction loaded = loader.loadClass(n.getName());

                        // Remove the last compilation error if any
                        unload(n.getName());

                        // Add the function back
                        addFunction(n.getName(), loaded);
                    }
                    catch (CompilationFailedException exc)
                    {
                        if (config.isExposeCompilationError())
                        {
                            // Add an end-point under /error/{name} to know the compilation error
                            addFunction(n.getName(), new LastCompilationErrorFunction(n.getName(), exc));
                        }
                        throw exc;
                    }
                    break;

                case DELETED:
                    if (config.isUnloadOnRemoval())
                    {
                        unload(n.getName());
                    }
            }
        });
    }

    private void addFunction(String filename, ServerFunction func)
    {
        final boolean exists = functions.put(func.getClass().getName(), func) != null;
        logger.info(exists ? "'{}' was reloaded" : "'{}' was loaded", filename);
    }

    @PostConstruct
    protected void loadAll()
    {
        for (HandlerFunctionInfo f : loader.findAll(0, Integer.MAX_VALUE))
        {
            try
            {
                addFunction(f.getName(), loader.loadClass(f.getName()));
            }
            catch (Exception exc)
            {
                logger.error("Error in function {}: {}", f.getName(), exc.getMessage());
            }
        }
    }

    private void unload(final String name)
    {
        final ServerFunction func = functions.remove(name);
        if (func != null)
        {
            logger.info("'{}' was unloaded", name);
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response)
    {
        for (final ServerFunction serverFunction : functions.values())
        {
            if (doHandle(request, response, serverFunction))
            {
                return;
            }
        }

        if (new ApiSpecFunction(functions, loader).handle(request, response) == FunctionResult.PROCESSED)
        {
            return;
        }

        if (new ApiDocFunction().handle(request, response) == FunctionResult.PROCESSED)
        {
            return;
        }

        response.error(ErrorResponse.notFound("No function found to handle '" + request.path() + "'"));
    }

    private boolean doHandle(HttpRequest request, HttpResponse response, ServerFunction f)
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
