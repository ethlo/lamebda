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
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.FunctionManagerDirector;
import com.ethlo.lamebda.ProjectCleanupService;
import com.ethlo.lamebda.ProjectSetupService;
import com.ethlo.lamebda.loader.ProjectLoader;
import com.ethlo.lamebda.loader.http.HttpRepositoryProjectLoader;
import com.ethlo.lamebda.util.IoUtil;

@Configuration
@ConfigurationProperties("lamebda")
@ConditionalOnProperty(prefix = "lamebda", name = "enabled")
public class LamebdaSpringWebAutoConfiguration
{
    @Value("${lamebda.request-path:/lamebda}")
    private String rootContextPath;

    @Value("${lamebda.source.directory:/lamebda}")
    private Path rootDir;

    @Value("${lamebda.source.projects:null}")
    private Set<String> projectNames;

    @Autowired
    private ConfigurableApplicationContext parentContext;

    @Autowired(required = false)
    private List<MethodInterceptor> methodInterceptors = new LinkedList<>();

    public void setRootContextPath(String rootContextPath)
    {
        this.rootContextPath = rootContextPath;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("lamebda.enabled")
    public FunctionManagerDirector functionManagerDirector() throws IOException
    {
        final String configServerUrl = parentContext.getEnvironment().getProperty("spring.cloud.config.uri");
        final String applicationName = parentContext.getEnvironment().getProperty("spring.application.name");

        if (StringUtils.hasLength(configServerUrl) && StringUtils.hasLength(applicationName))
        {
            final ProjectLoader projectLoader = new HttpRepositoryProjectLoader(
                    rootDir,
                    IoUtil.stringToURL(configServerUrl),
                    applicationName,
                    "default",
                    "master",
                    projectNames
            );
        }

        return new FunctionManagerDirector(rootDir, rootContextPath, parentContext);
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
