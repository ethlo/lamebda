package com.ethlo.lamebda.spring;

/*-
 * #%L
 * lamebda-spring-web-starter
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class LamebdaHandlerMapping extends RequestMappingHandlerMapping
{
    private String requestPrefix;
    private HandlerMethod handler;

    public LamebdaHandlerMapping(LamebdaController controller, String requestPrefix)
    {
        setOrder(HIGHEST_PRECEDENCE);
        this.requestPrefix = requestPrefix;
        try
        {
            this.handler = new InvocableHandlerMethod(controller, "handle", HttpServletRequest.class, HttpServletResponse.class);
        }
        catch (NoSuchMethodException exc)
        {
            throw new IllegalStateException(exc);
        }
    }
    
    @Override
    protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception
    {
        final String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        if (requestPath.startsWith(requestPrefix))
        {
            return handler; 
        }
        return null;
    }
}

