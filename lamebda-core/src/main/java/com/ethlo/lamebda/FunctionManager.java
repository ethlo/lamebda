package com.ethlo.lamebda;

import java.lang.reflect.UndeclaredThrowableException;

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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import com.ethlo.lamebda.error.ErrorResponse;
import com.google.common.base.Throwables;

public class FunctionManager
{
    protected static final Logger logger = LoggerFactory.getLogger(FunctionManager.class);
    
    private Map<String, ServerFunction> functions = new ConcurrentHashMap<>();
    private ClassResourceLoader loader;
    
    public FunctionManager(ClassResourceLoader loader)
    {
        this.loader = loader;
        loader.setChangeListener(n->addFunction(loader.loadClass(n.getName())));
    }
    
    private void addFunction(ServerFunction func)
    {
        final boolean exists = functions.put(func.getClass().getName(), func) != null;
        logger.info(exists ? "{} was modified" : "{} was loaded", func.getClass().getSimpleName());
    }
    
    @PostConstruct
    protected void loadAll()
    {
        for (HandlerFunctionInfo f : loader.findAll(new PageRequest(0, Integer.MAX_VALUE)))
        {
            addFunction(loader.loadClass(f.getName()));
        }
    }

    public void handle(HttpRequest request, HttpResponse response)
    {
        final Iterator<ServerFunction> iter = functions.values().iterator();
        while (iter.hasNext())
        {
            if (doHandle(request, response, iter.next()))
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
            return f.handle(request, response);
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
        throw Throwables.propagate(cause);
    }

}
