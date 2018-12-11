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
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.HandlerMapping;

import com.ethlo.lamebda.ApiSpecLoader;
import com.ethlo.lamebda.ClassResourceLoader;
import com.ethlo.lamebda.FunctionManager;
import com.ethlo.lamebda.FunctionManagerConfig;
import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.loaders.FileSystemClassResourceLoader;
import com.ethlo.lamebda.loaders.FunctionPostProcesor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ConfigurationProperties("lamebda")
@ConditionalOnProperty(prefix="lamebda", name="enabled")
public class LamebdaSpringWebAutoConfiguration
{
    public static final String DEFAULT_PATH = "/lamebda";
    
    private static final Logger logger = LoggerFactory.getLogger(LamebdaSpringWebAutoConfiguration.class);
    
    private String requestPath = DEFAULT_PATH;

    @Autowired
    private ApplicationContext applicationContext;

    public void setRequestPath(String requestPath)
    {
        this.requestPath = requestPath;
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionPostProcesor functionPostProcessor()
    {
        return AutowireHelper.process(applicationContext);
    }

    @Validated
    @Component
    @ConditionalOnMissingBean(ClassResourceLoader.class)
    @ConditionalOnProperty("lamebda.source.directory")
    @ConfigurationProperties("lamebda.source")
    public class FileSourceConfiguration
    {
        @NotNull
        private String directory;

        public String getDirectory()
        {
            return directory;
        }

        public void setDirectory(String directory)
        {
            this.directory = directory;
        }
    }
    
    @Bean
    @ConditionalOnBean(FileSourceConfiguration.class)
    public ClassResourceLoader classResourceLoader(FunctionPostProcesor functionPostProcesor, FileSourceConfiguration cfg) throws IOException
    {
        logger.info("Using file system class loader");
        return new FileSystemClassResourceLoader(functionPostProcesor, cfg.getDirectory());
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionManagerConfig functionManagerConfig()
    {
        return new FunctionManagerConfig();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ClassResourceLoader.class)
    public FunctionManager functionManager(ClassResourceLoader classResourceLoader, ApiSpecLoader apiSpecLoader, FunctionManagerConfig functionManagerConfig)
    {
        return new FunctionManagerImpl(classResourceLoader, apiSpecLoader, functionManagerConfig);
    }

    @Bean
    @ConditionalOnBean(FunctionManager.class)
    public LamebdaController lamebdaController(FunctionManager functionManager, ObjectMapper mapper)
    {
        return new LamebdaController(functionManager, requestPath, mapper);
    }
    
    @Bean
    @ConditionalOnBean(LamebdaController.class)
    public HandlerMapping lamebdaHandlerMapping(LamebdaController handler)
    {
        logger.info("Registering handler mapping for request path prefix: {}", requestPath);
        return new LamebdaHandlerMapping(handler, requestPath);
    }
}
