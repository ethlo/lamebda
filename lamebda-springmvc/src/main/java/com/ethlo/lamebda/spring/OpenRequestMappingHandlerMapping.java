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
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.core.env.PropertyResolver;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class OpenRequestMappingHandlerMapping extends RequestMappingHandlerMapping
{
    public RequestMappingInfo getMappingForMethod(final PropertyResolver propertyResolver, final Method method, final Object handler)
    {
        Class<?> objClz = handler.getClass();
        if (org.springframework.aop.support.AopUtils.isAopProxy(handler))
        {
            objClz = org.springframework.aop.support.AopUtils.getTargetClass(handler);
        }

        final RequestMappingInfo mapping = super.getMappingForMethod(method, objClz);
        if (mapping != null)
        {
            final String[] paths = mapping.getPatternValues()
                    .stream()
                    .map(propertyResolver::resolveRequiredPlaceholders)
                    .map(OpenRequestMappingHandlerMapping::normalizeSlashes)
                    .toArray(String[]::new);

            return mapping.mutate().paths(paths).build();
        }
        return null;
    }

    public static String normalizeSlashes(final String path)
    {
        return Arrays.stream(path.split("/")).filter(s -> !"".equals(s)).collect(Collectors.joining("/"));
    }
}
