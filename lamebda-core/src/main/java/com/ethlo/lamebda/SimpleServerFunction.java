package com.ethlo.lamebda;

import java.util.Collections;
import java.util.Map;

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

public abstract class SimpleServerFunction implements ServerFunction
{
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private final String pattern;

    public SimpleServerFunction(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public FunctionResult handle(HttpRequest request, HttpResponse response)
    {
        if (! PATH_MATCHER.match(pattern, request.path()))
        {
            return FunctionResult.SKIPPED;
        }
        
        doHandle(request, response);
        return FunctionResult.PROCESSED;
    }
    
    protected void doHandle(HttpRequest request, HttpResponse response)
    {
        final HttpMethod m = HttpMethod.parse(request.method());
        if (m == null)
        {
            response.error(new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Cannot handle HTTP method: " + request.method()));
            return;
        }
        
        switch (m)
        {
            case POST:
                post(request, response);
                break;
            case CONNECT:
                connect(request, response);
                break;
            case DELETE:
                delete(request, response);
                break;
            case GET:
                get(request, response);
                break;
            case HEAD:
                head(request, response);
                break;
            case OPTIONS:
                options(request, response);
                break;
            case PATCH:
                patch(request, response);
                break;
            case PUT:
                put(request, response);
                break;
            case TRACE:
                trace(request, response);
                break;
        }
    }
    
    protected void post(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void get(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void connect(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void put(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void delete(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void head(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void options(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void patch(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected void trace(HttpRequest request, HttpResponse response)
    {
        
    }
    
    protected Map<String, String> getPathVars(String pattern, HttpRequest request)
    {
        try
        {
            return PATH_MATCHER.extractUriTemplateVariables(pattern, request.path());
        }
        catch (IllegalStateException exc)
        {
            return Collections.emptyMap();
        }
    }
}
