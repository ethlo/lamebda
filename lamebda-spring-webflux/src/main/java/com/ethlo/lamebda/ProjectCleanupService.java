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

import static com.ethlo.lamebda.spring.RequestMappingInfoUtil.normalizeSlashes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import com.ethlo.lamebda.lifecycle.ProjectClosingEvent;
import com.ethlo.lamebda.lifecycle.ProjectEvent;

public class ProjectCleanupService implements ApplicationListener<ProjectClosingEvent>
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectCleanupService.class);

    public static List<RequestMappingHandlerMapping> getMappingHandler(ProjectEvent event)
    {
        final AnnotationConfigApplicationContext ctx = event.getProjectContext();
        return new ArrayList<>(BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx.getBeanFactory(), RequestMappingHandlerMapping.class).values());
    }

    private boolean isProjectMapped(final String prefix, final RequestMappingInfo mappingInfo)
    {
        for (String pattern : mappingInfo.getPatternsCondition().getPatterns().stream().map(PathPattern::getPatternString).collect(Collectors.toSet()))
        {
            if (normalizeSlashes(pattern).startsWith(normalizeSlashes(prefix)))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onApplicationEvent(final ProjectClosingEvent event)
    {
        final ProjectConfiguration projectConfiguration = event.getProjectConfiguration();
        final List<RequestMappingHandlerMapping> mappingHandlers = getMappingHandler(event);
        mappingHandlers.forEach(mappingHandler ->
        {
            final Map<RequestMappingInfo, HandlerMethod> handlerMethods = mappingHandler.getHandlerMethods();
            final String prefix = projectConfiguration.getRootContextPath() + "/" + projectConfiguration.getContextPath();
            final List<RequestMappingInfo> toRemove = new LinkedList<>();
            handlerMethods.forEach((key, value) -> {
                if (isProjectMapped(prefix, key))
                {
                    toRemove.add(key);
                }
            });

            toRemove.forEach(key -> {
                logger.info("Unregistering {}", key);
                mappingHandler.unregisterMapping(key);
            });
        });
    }
}
