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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import com.ethlo.lamebda.lifecycle.ProjectLoadedEvent;
import com.ethlo.lamebda.mapping.RequestMapping;
import com.ethlo.lamebda.spring.RequestMappingInfoUtil;

public class ProjectSetupService implements ApplicationListener<ProjectLoadedEvent>
{
    private static final Logger logger = LoggerFactory.getLogger(ProjectSetupService.class);
    private final ListableBeanFactory beanFactory;
    private final List<MethodInterceptor> methodInterceptors;

    public ProjectSetupService(final ListableBeanFactory beanFactory, final List<MethodInterceptor> methodInterceptors)
    {
        this.beanFactory = beanFactory;
        this.methodInterceptors = methodInterceptors;
    }

    private List<RequestMapping> register(PropertyResolver propertyResolver, Collection<RequestMappingHandlerMapping> handlerMappings, Object controller, ProjectConfiguration projectConfiguration)
    {
        final Object wrappedController = wrapController(controller);

        final List<RequestMapping> result = new LinkedList<>();

        final Class<?> userType = ClassUtils.getUserClass(controller.getClass());

        final Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(userType, (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method ->
                {
                    try
                    {
                        return RequestMappingInfoUtil.getMappingForMethod(projectConfiguration, propertyResolver, userType, method);
                    }
                    catch (Throwable ex)
                    {
                        throw new IllegalStateException("Invalid mapping on handler class [" +
                                userType.getName() + "]: " + method, ex);
                    }
                }
        );

        handlerMappings.forEach(handlerMapping -> methods.forEach((method, mapping) -> result.add(doRegister(handlerMapping, wrappedController, method, mapping))));
        return result;
    }

    private Object wrapController(final Object controller)
    {
        final BeanFactoryAspectJAdvisorsBuilder advisorsBuilder = new BeanFactoryAspectJAdvisorsBuilder(beanFactory);
        final List<Advisor> advisors = advisorsBuilder.buildAspectJAdvisors();
        return createAOPProxyWithInterceptorsAndAdvisors(methodInterceptors, advisors, controller);
    }

    private RequestMapping doRegister(final RequestMappingHandlerMapping handlerMapping, final Object object, final Method m, final RequestMappingInfo mappingToUse)
    {

        final Set<HttpMethod> methods = mappingToUse.getMethodsCondition().getMethods().stream().map(method -> HttpMethod.parse(method.name())).collect(Collectors.toSet());
        final Set<String> patterns = mappingToUse.getPatternsCondition().getPatterns().stream().map(PathPattern::getPatternString).collect(Collectors.toSet());
        final Set<String> consumes = mappingToUse.getConsumesCondition().getConsumableMediaTypes().stream().map(MimeType::toString).collect(Collectors.toSet());
        final Set<String> produces = mappingToUse.getProducesCondition().getProducibleMediaTypes().stream().map(MimeType::toString).collect(Collectors.toSet());

        logger.info("Registering {}", mappingToUse);
        handlerMapping.unregisterMapping(mappingToUse);
        handlerMapping.registerMapping(mappingToUse, object, m);

        return new RequestMapping(patterns, methods, consumes, produces);
    }

    private Object createAOPProxyWithInterceptorsAndAdvisors(final List<MethodInterceptor> methodInterceptors, final List<Advisor> advisors, Object controller)
    {
        final ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.setProxyClassLoader(controller.getClass().getClassLoader());
        proxyFactoryBean.setTargetSource(new SingletonTargetSource(controller));
        proxyFactoryBean.addAdvisors(advisors);
        methodInterceptors.forEach(proxyFactoryBean::addAdvice);
        return proxyFactoryBean.getObject();
    }

    @Override
    public void onApplicationEvent(final ProjectLoadedEvent event)
    {
        final AnnotationConfigApplicationContext projectCtx = event.getProjectContext();
        final ProjectConfiguration projectCfg = event.getProjectConfiguration();

        final List<RequestMappingHandlerMapping> handlerMappings = ProjectCleanupService.getMappingHandler(event);

        // Register controller beans
        final SortedSet<RequestMapping> allMappings = new TreeSet<>();
        projectCtx.getBeansWithAnnotation(Controller.class).forEach((beanName, controller) ->
        {
            final List<RequestMapping> mappings = register(projectCtx.getEnvironment(), handlerMappings, controller, projectCfg);
            allMappings.addAll(mappings);
        });

        event.getProjectContext().registerBean("_all_mappings", Set.class, () -> allMappings);
    }
}
