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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import com.ethlo.lamebda.error.HttpError;

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
            final ServerFunction f = iter.next();
            if (f.handle(request, response))
            {
                return;
            }
        }
        
        logger.info("No function found to handle: {}", request.path());
        final ServerFunction f = (req, res) ->{
            res.respond(HttpError.E404);
            return true;
        };
        f.handle(request, response);
    }
}
