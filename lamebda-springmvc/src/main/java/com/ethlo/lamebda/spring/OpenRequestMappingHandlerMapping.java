package com.ethlo.lamebda.spring;

/*-
 * #%L
 * lamebda-springmvc
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
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

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class OpenRequestMappingHandlerMapping extends RequestMappingHandlerMapping
{
    // NOTE. Leave here to NOT make it look like we do a lot of stuff from Lamebda, due to the logger being inherited and initialized with getClass()
    private final Log logger = LogFactory.getLog(super.getClass());

    @Override
    public RequestMappingInfo getMappingForMethod(final Method method, final Class<?> handlerType)
    {
        return super.getMappingForMethod(method, handlerType);
    }
}
