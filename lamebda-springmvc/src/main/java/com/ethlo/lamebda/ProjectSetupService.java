package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-spring-web-starter
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.MimeType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.ethlo.lamebda.functions.ProjectStatusFunction;
import com.ethlo.lamebda.lifecycle.ProjectLoadedEvent;
import com.ethlo.lamebda.mapping.RequestMapping;
import com.ethlo.lamebda.spring.OpenRequestMappingHandlerMapping;

public class ProjectSetupService implements ApplicationListener<ProjectLoadedEvent>
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectSetupService.class);

    private final OpenRequestMappingHandlerMapping openRequestMappingHandlerMapping = new OpenRequestMappingHandlerMapping();

    private List<RequestMapping> register(RequestMappingHandlerMapping handlerMapping, Object controller, ProjectConfiguration projectConfiguration)
    {
        final List<RequestMapping> result = new LinkedList<>();
        Arrays.asList(ReflectionUtils.getUniqueDeclaredMethods(controller.getClass())).forEach(method ->
        {
            final RequestMappingInfo mapping = openRequestMappingHandlerMapping.getMappingForMethod(method, controller);
            if (mapping != null)
            {
                final RequestMapping res = doRegister(handlerMapping, controller, method, mapping, projectConfiguration);
                result.add(res);
            }
        });
        return result;
    }

    private RequestMapping doRegister(final RequestMappingHandlerMapping handlerMapping, final Object object, final Method m, final RequestMappingInfo mapping, ProjectConfiguration projectConfiguration)
    {
        final String rootContextPath = projectConfiguration.getRootContextPath();
        RequestMappingInfo mappingToUse = RequestMappingInfo.paths(rootContextPath).build();

        if (projectConfiguration.enableUrlProjectContextPrefix())
        {
            final String projectContextPath = projectConfiguration.getContextPath();
            mappingToUse = mappingToUse.combine(RequestMappingInfo.paths(projectContextPath).build());
        }
        mappingToUse = mappingToUse.combine(mapping);
        final Set<HttpMethod> methods = mappingToUse.getMethodsCondition().getMethods().stream().map(method -> HttpMethod.parse(method.name())).collect(Collectors.toSet());
        final Set<String> patterns = mappingToUse.getPatternsCondition().getPatterns();
        final Set<String> consumes = mappingToUse.getConsumesCondition().getConsumableMediaTypes().stream().map(MimeType::toString).collect(Collectors.toSet());
        final Set<String> produces = mappingToUse.getProducesCondition().getProducibleMediaTypes().stream().map(MimeType::toString).collect(Collectors.toSet());

        logger.info("Registering {}", mappingToUse);
        handlerMapping.unregisterMapping(mappingToUse);
        handlerMapping.registerMapping(mappingToUse, object, m);

        return new RequestMapping(patterns, methods, consumes, produces);
    }

    @Override
    public void onApplicationEvent(final ProjectLoadedEvent event)
    {
        final AnnotationConfigApplicationContext projectCtx = event.getProjectCtx();
        final ProjectConfiguration projectCfg = event.getProjectConfiguration();

        final RequestMappingHandlerMapping handlerMapping = projectCtx.getBean(RequestMappingHandlerMapping.class);

        final ProjectStatusFunction psf = new ProjectStatusFunction(projectCfg);
        register(handlerMapping, psf, projectCfg);

        // Register controller beans
        projectCtx.getBeansOfType(ServerFunction.class).forEach((beanName, controller) ->
        {
            final List<RequestMapping> mappings = register(handlerMapping, controller, projectCfg);
            if (!mappings.isEmpty())
            {
                psf.add(beanName, mappings);
            }
        });
    }
}
