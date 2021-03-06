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
import java.util.LinkedList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.ProjectCleanupService;
import com.ethlo.lamebda.ProjectManager;
import com.ethlo.lamebda.ProjectSetupService;
import com.ethlo.lamebda.loader.http.HttpCloudConfigLoader;
import com.ethlo.lamebda.util.IoUtil;

@Configuration
@EnableConfigurationProperties(LamebdaRootConfiguration.class)
@ConditionalOnProperty(prefix = "lamebda", name = "enabled")
public class LamebdaSpringWebAutoConfiguration
{
    @Autowired
    private LamebdaRootConfiguration lamebdaRootConfiguration;

    @Autowired
    private ConfigurableApplicationContext parentContext;

    @Autowired(required = false)
    private List<MethodInterceptor> methodInterceptors = new LinkedList<>();

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("lamebda.enabled")
    public ProjectManager projectManager() throws IOException
    {
        deploy();

        return new ProjectManager(lamebdaRootConfiguration.getRootDirectory(), lamebdaRootConfiguration.getRequestPath(), parentContext);
    }

    private void deploy()
    {
        final String configServerUrl = getProperty("spring.cloud.config.uri");
        final String applicationName = getProperty("spring.application.name");
        final String profileName = getProperty("spring.profiles.active") != null ? StringUtils.commaDelimitedListToSet(getProperty("spring.profiles.active")).iterator().next() : "default";
        final String labelName = getProperty("spring.cloud.config.label") != null ? getProperty("spring.cloud.config.label") : "master";

        if (StringUtils.hasLength(configServerUrl) && StringUtils.hasLength(applicationName))
        {
            new HttpCloudConfigLoader(
                    lamebdaRootConfiguration.getRootDirectory(),
                    IoUtil.stringToURL(configServerUrl),
                    applicationName,
                    profileName,
                    labelName,
                    lamebdaRootConfiguration.getProjectNames()
            ).prepareConfig();
        }
    }

    private String getProperty(final String s)
    {
        return parentContext.getEnvironment().getProperty(s);
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
