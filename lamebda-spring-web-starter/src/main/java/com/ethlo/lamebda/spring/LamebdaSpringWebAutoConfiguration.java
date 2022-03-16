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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ethlo.lamebda.LamebdaConfiguration;
import com.ethlo.lamebda.LamebdaMetaAccessService;
import com.ethlo.lamebda.LamebdaMetaAccessServiceImpl;
import com.ethlo.lamebda.ProjectCleanupService;
import com.ethlo.lamebda.ProjectManager;
import com.ethlo.lamebda.ProjectSetupService;
import com.ethlo.lamebda.functions.ProjectInfoController;

@Configuration
@EnableConfigurationProperties(LamebdaConfiguration.class)
@ConditionalOnProperty(prefix = "lamebda", name = "enabled")
public class LamebdaSpringWebAutoConfiguration
{
    private final List<MethodInterceptor> methodInterceptors;
    private final LamebdaConfiguration lamebdaConfiguration;
    private final ConfigurableApplicationContext parentContext;

    public LamebdaSpringWebAutoConfiguration(@Autowired(required = false) List<MethodInterceptor> methodInterceptors, LamebdaConfiguration lamebdaConfiguration, ConfigurableApplicationContext parentContext)
    {
        this.methodInterceptors = Optional.ofNullable(methodInterceptors).orElse(Collections.emptyList());
        this.lamebdaConfiguration = lamebdaConfiguration;
        this.parentContext = parentContext;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("lamebda.enabled")
    public ProjectManager projectManager() throws IOException
    {
        return new ProjectManager(lamebdaConfiguration, parentContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public LamebdaMetaAccessService lamebdaMetaAccessService(final LamebdaConfiguration lamebdaConfig)
    {
        return new LamebdaMetaAccessServiceImpl(lamebdaConfig);
    }

    @Bean
    public ProjectInfoController projectInfoController(final ProjectManager projectManager, final LamebdaMetaAccessService lamebdaMetaAccessService)
    {
        return new ProjectInfoController(projectManager, lamebdaMetaAccessService);
    }

    @Bean
    public ProjectSetupService projectSetupService()
    {
        return new ProjectSetupService(parentContext, methodInterceptors);
    }

    @Bean
    public ProjectCleanupService projectCleanupService()
    {
        return new ProjectCleanupService();
    }
}
