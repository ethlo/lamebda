package com.ethlo.lamebda.spring;

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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.lifecycle.ProjectClosingEvent;

public class ProjectCleanupService implements ApplicationListener<ProjectClosingEvent>
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectCleanupService.class);

    private boolean isProjectMapped(final String prefix, final RequestMappingInfo mappingInfo)
    {
        for (String pattern : mappingInfo.getPatternsCondition().getPatterns())
        {
            if (pattern.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onApplicationEvent(final ProjectClosingEvent event)
    {
        final ProjectConfiguration projectConfiguration = event.getProjectCtx().getBean(ProjectConfiguration.class);
        final RequestMappingHandlerMapping mappingHandler = event.getProjectCtx().getBean(RequestMappingHandlerMapping.class);
        final Map<RequestMappingInfo, HandlerMethod> handlerMethods = mappingHandler.getHandlerMethods();
        final String prefix = projectConfiguration.getRootContextPath() + "/" + projectConfiguration.getContextPath();
        handlerMethods.forEach((key, value) -> {
            if (isProjectMapped(prefix, key))
            {
                logger.info("Unregistering {}, ", key);
                mappingHandler.unregisterMapping(key);
            }
        });
    }
}
