package com.ethlo.lamebda;

import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.util.IoUtil;
import com.ethlo.lamebda.util.StringUtil;

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
                    addFunction(loader.loadClass(n.getName()));
                    break;

                case DELETED:
                    unload(n.getName());
            }
        });
    }

    private void addFunction(ServerFunction func)
    {
        final boolean exists = functions.put(func.getClass().getName(), func) != null;
        logger.info(exists ? "{} was modified" : "{} was loaded", func.getClass().getSimpleName());
    }

    @PostConstruct
    protected void loadAll()
    {
        for (HandlerFunctionInfo f : loader.findAll(0, Integer.MAX_VALUE))
        {
            try
            {
                addFunction(loader.loadClass(f.getName()));
            }
            catch (Exception exc)
            {
                if (config.isUnloadOnFunctionError())
                {
                    unload(f.getName());
                }
                logger.error("Error in function {}: {}", f.getName(), exc.getMessage());
            }
        }
    }

    private void unload(final String name)
    {
        final ServerFunction func = functions.remove(name);
        if (func != null)
        {
            logger.info("{} was unloaded", name);
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response)
    {
        final SimpleServerFunction docFunc = new SimpleServerFunction("/doc/*")
        {
            @Override
            protected void get(HttpRequest request, HttpResponse response)
            {
                final String docPage = IoUtil.classPathResourceAsString("doc.html", StandardCharsets.UTF_8);
                response.setContentType(HttpMimeType.HTML);
                response.write(docPage);
            }
        };

        final SimpleServerFunction specFunc = new SimpleServerFunction("/doc/*.json")
        {
            @Override
            protected void get(HttpRequest request, HttpResponse response)
            {
                final String name = getPathVars("/doc/{function}.*", request).get("function");
                final String functionName = StringUtil.hyphenToCamelCase(name);
                final boolean functionExists = functions.containsKey(functionName);
                if (functionExists)
                {
                    response.write(loader.loadApiSpec(functionName));
                }
                else
                {
                    response.error(HttpStatus.NOT_FOUND, "No API documentation file was found");
                }
            }
        };

        if (specFunc.handle(request, response) == FunctionResult.PROCESSED)
        {
            return;
        }

        if (docFunc.handle(request, response) == FunctionResult.PROCESSED)
        {
            return;
        }

        for (final ServerFunction serverFunction : functions.values())
        {
            if (doHandle(request, response, serverFunction))
            {
                return;
            }
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

    public Map<String,ServerFunction> getFunctions()
    {
        return Collections.unmodifiableMap(functions);
    }
}
