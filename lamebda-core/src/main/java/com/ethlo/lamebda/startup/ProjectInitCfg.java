package com.ethlo.lamebda.startup;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 - 2024 Morten Haraldsen (ethlo)
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ethlo.lamebda.ProjectManager;

@Configuration
public class ProjectInitCfg
{
    @ConditionalOnProperty(prefix = "lamebda", name = "init-stage", havingValue = "direct")
    @Bean
    public DirectStartupListener directStartupListener(ProjectManager projectManager)
    {
        return new DirectStartupListener(projectManager);
    }

    @ConditionalOnProperty(prefix = "lamebda", name = "init-stage", havingValue = "started", matchIfMissing = true)
    @Bean
    public ApplicationStartedStartupListener applicationStartedStartupListener(ProjectManager projectManager)
    {
        return new ApplicationStartedStartupListener(projectManager);
    }

    @ConditionalOnProperty(prefix = "lamebda", name = "init-stage", havingValue = "ready")
    @Bean
    public ApplicationReadyStartupListener applicationReadyStartupListener(ProjectManager projectManager)
    {
        return new ApplicationReadyStartupListener(projectManager);
    }

}
