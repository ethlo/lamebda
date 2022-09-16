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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.util.pattern.PathPatternParser;

import com.ethlo.lamebda.ProjectConfiguration;

public class RequestMappingInfoUtil
{
    private static final RequestMappingInfo.BuilderConfiguration options;

    static
    {
        options = new RequestMappingInfo.BuilderConfiguration();
        options.setPatternParser(new PathPatternParser());
    }

    public static RequestMappingInfo getMappingForMethod(final ProjectConfiguration projectConfiguration, final PropertyResolver propertyResolver, final AnnotatedElement instance, final Method method)
    {
        final RequestMappingInfo methodLevel = getMapping(method, propertyResolver);
        if (methodLevel != null)
        {
            RequestMappingInfo mapping = methodLevel;
            final RequestMappingInfo typeInfo = getMapping(instance, propertyResolver);
            if (typeInfo != null)
            {
                mapping = typeInfo.combine(methodLevel);
            }

            // Project context path
            final RequestMappingInfo projectPathRequestMappingInfo = RequestMappingInfo
                    .paths(projectConfiguration.getContextPath())
                    .options(options).build();

            // Root context path
            final RequestMappingInfo rootPathRequestMappingInfo = RequestMappingInfo
                    .paths(projectConfiguration.getRootContextPath())
                    .options(options).build();

            return rootPathRequestMappingInfo.combine(projectPathRequestMappingInfo.combine(mapping));
        }
        return null;
    }

    private static RequestMappingInfo getMapping(AnnotatedElement userInstance, final PropertyResolver propertyResolver)
    {
        final RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(userInstance, RequestMapping.class);
        if (requestMapping != null)
        {
            return RequestMappingInfo
                    .paths(Arrays.stream(requestMapping.path()).map(propertyResolver::resolveRequiredPlaceholders).toArray(String[]::new))
                    .methods(requestMapping.method())
                    .params(requestMapping.params())
                    .headers(requestMapping.headers())
                    .consumes(requestMapping.consumes())
                    .produces(requestMapping.produces())
                    .mappingName(requestMapping.name())
                    .options(options)
                    .build();
        }
        return null;
    }

    public static String normalizeSlashes(final String path)
    {
        return Arrays.stream(path.split("/")).filter(s -> !"".equals(s)).collect(Collectors.joining("/"));
    }
}
