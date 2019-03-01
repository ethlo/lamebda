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
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerMapping;

import com.ethlo.lamebda.FunctionManagerDirector;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.servlet.LamebdaMetricsFilter;
import com.ethlo.lamebda.util.StringUtil;

@Configuration
@ConfigurationProperties("lamebda")
@ConditionalOnProperty(prefix = "lamebda", name = "enabled")
public class LamebdaSpringWebAutoConfiguration
{
    private static final Logger logger = LoggerFactory.getLogger(LamebdaSpringWebAutoConfiguration.class);

    @Value("${lamebda.request-path:/lamebda}")
    private String rootContextPath;

    @Value("${lamebda.source.directory:/lamebda}")
    private Path rootDir;

    @Autowired
    private ApplicationContext parentContext;

    public void setRootContextPath(String rootContextPath)
    {
        this.rootContextPath = rootContextPath;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("lamebda.enabled")
    public FunctionManagerDirector functionManagerDirector() throws IOException
    {
        return new FunctionManagerDirector(rootDir, rootContextPath, parentContext);
    }

    @Bean
    public FilterRegistrationBean metricsFilter()
    {
        final FilterRegistrationBean<LamebdaMetricsFilter> b = new FilterRegistrationBean<>();
        b.setFilter(new LamebdaMetricsFilter(FunctionMetricsService.getInstance()));
        final String urlPattern = "/" + StringUtil.strip(this.rootContextPath, "/") + "/*";
        b.addUrlPatterns(urlPattern);
        b.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        return b;
    }

    @Bean
    public LamebdaController lamebdaController(FunctionManagerDirector functionManagerDirector)
    {
        return new LamebdaController(functionManagerDirector, rootContextPath);
    }

    @Bean
    @ConditionalOnBean(LamebdaController.class)
    public HandlerMapping lamebdaHandlerMapping(LamebdaController handler)
    {
        logger.info("Registering handler mapping for request path prefix: {}", rootContextPath);
        return new LamebdaHandlerMapping(handler, rootContextPath);
    }
}
