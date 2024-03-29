package com.ethlo.lamebda;

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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ethlo.lamebda.functions.ProjectInfoController;

@Configuration
public class TestCfg
{
    @Autowired(required = false)
    private List<MethodInterceptor> methodInterceptors = new LinkedList<>();

    @Bean
    public ProjectManager projectManager(ApplicationContext applicationContext) throws IOException
    {
        final Path basePath = Paths.get("src/test/projects");
        final LamebdaConfiguration lamebdaConfiguration = new LamebdaConfiguration("lamebda", "classpath:foo", "swaggerUiPath", true, basePath, true);
        return new ProjectManager(lamebdaConfiguration, applicationContext);
    }

    @Bean
    public LamebdaMetaAccessService lamebdaMetaAccessService()
    {
        return new AllowAllLamebdaMetaAccessService();
    }

    @Bean
    public ProjectInfoController deploymentStatusController(final ProjectManager projectManager, final LamebdaMetaAccessService lamebdaMetaAccessService)
    {
        return new ProjectInfoController(projectManager, lamebdaMetaAccessService);
    }

    @Bean
    public ProjectSetupService projectSetupService(final ApplicationContext parentContext)
    {
        return new ProjectSetupService(parentContext, methodInterceptors);
    }

    @Bean
    public ProjectCleanupService projectCleanupService()
    {
        return new ProjectCleanupService();
    }
}
