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

import static org.springframework.beans.factory.wiring.BeanWiringInfo.AUTOWIRE_BY_NAME;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerMapping;

import com.ethlo.lamebda.BaseServerFunction;
import com.ethlo.lamebda.ConfigurableFunctionManager;
import com.ethlo.lamebda.DelegatingFunctionManager;
import com.ethlo.lamebda.FunctionManagerDirector;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.FunctionPostProcessor;
import com.ethlo.lamebda.loaders.FunctionSourcePreProcessor;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.servlet.LamebdaMetricsFilter;
import com.ethlo.lamebda.util.StringUtil;
import groovy.lang.GroovyClassLoader;

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

    public void setRootContextPath(String rootContextPath)
    {
        this.rootContextPath = rootContextPath;
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionSourcePreProcessor functionLoadPreNotification()
    {
        return ((classLoader, source) -> source);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("lamebda.enabled")
    public FunctionManagerDirector functionManagerDirector() throws IOException
    {
        FunctionPostProcessor funcPostProcessor = function ->
        {
            if (function instanceof SpringMvcServerFunction)
            {
                final SpringMvcServerFunction springMvcServerFunction = (SpringMvcServerFunction) function;
                final AnnotationConfigApplicationContext projectCtx = springMvcServerFunction.getApplicationContext();
                AutowireHelper.postProcessor(projectCtx).process(function);
            }

            if (function instanceof BaseServerFunction)
            {
                ((BaseServerFunction) function).handlePostConstructMethods();
            }
            return function;
        };

        return new FunctionManagerDirector(rootDir, rootContextPath, funcPostProcessor)
        {
            @Override
            protected void postInit(final ConfigurableFunctionManager functionManager)
            {
                final AnnotationConfigApplicationContext projectCtx = new AnnotationConfigApplicationContext();
                projectCtx.setId(functionManager.getProjectConfiguration().getName());
                projectCtx.setClassLoader(functionManager.getClassLoader());
                projectCtx.refresh();
                findSharedClasses(projectCtx, functionManager.getProjectConfiguration().getSharedPath());
            }
        };
    }

    private void findSharedClasses(AnnotationConfigApplicationContext projectCtx, Path sharedScriptsPath)
    {
        final GroovyClassLoader groovyClassLoader = (GroovyClassLoader) projectCtx.getClassLoader();

        try (Stream<Path> stream = Files.walk(sharedScriptsPath))
        {
            stream.forEach(e ->
            {
                if (e.getFileName().toString().endsWith(FileSystemLamebdaResourceLoader.SCRIPT_EXTENSION) && Files.isRegularFile(e))
                {
                    //final String className = FileSystemLamebdaResourceLoader.toClassName(sharedScriptsPath, e);
                    try
                    {
                        final Class<?> clazz = groovyClassLoader.parseClass(e.toFile());
                        registerIfSpringBean(clazz, projectCtx);
                    }
                    catch (ClassNotFoundException exc)
                    {
                        throw new RuntimeException(exc);
                    }
                    catch (IOException exc)
                    {
                        throw new UncheckedIOException(exc);
                    }
                }
            });
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    private void registerIfSpringBean(Class<?> clazz, final AnnotationConfigApplicationContext projectCtx) throws ClassNotFoundException
    {
        final String className = clazz.getCanonicalName();
        final AbstractBeanDefinition beanDef = BeanDefinitionReaderUtils.createBeanDefinition(null, className, projectCtx.getClassLoader());
        final AutowireCapableBeanFactory factory = projectCtx.getAutowireCapableBeanFactory();
        projectCtx.registerBeanDefinition(className, beanDef);
        Object bean = factory.createBean(clazz, AUTOWIRE_BY_NAME, false);
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
        return new LamebdaController(new DelegatingFunctionManager(functionManagerDirector), rootContextPath);
    }

    @Bean
    @ConditionalOnBean(LamebdaController.class)
    public HandlerMapping lamebdaHandlerMapping(LamebdaController handler)
    {
        logger.info("Registering handler mapping for request path prefix: {}", rootContextPath);
        return new LamebdaHandlerMapping(handler, rootContextPath);
    }
}
