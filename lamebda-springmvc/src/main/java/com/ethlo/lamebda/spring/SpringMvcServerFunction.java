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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.ethlo.lamebda.FunctionContextAware;
import com.ethlo.lamebda.FunctionResult;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.context.FunctionContext;

public class SpringMvcServerFunction extends RequestMappingHandlerMapping implements ServerFunction, FunctionContextAware
{
    protected final Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

    private FunctionContext context;

    @Autowired
    private RequestMappingHandlerAdapter adapter;

    @Autowired(required = false)
    private void postConstruct(final ListableBeanFactory beanFactory, final List<MethodInterceptor> methodInterceptors)
    {
        final BeanFactoryAspectJAdvisorsBuilder advisorsBuilder = new BeanFactoryAspectJAdvisorsBuilder(beanFactory);
        final List<Advisor> advisors = advisorsBuilder.buildAspectJAdvisors();

        final Object proxyObject = createAOPProxyWithInterceptorsAndAdvisors(methodInterceptors, advisors);
        detectAndRegisterRequestHandlerMethods(this.getClass(), proxyObject);
    }

    private Object createAOPProxyWithInterceptorsAndAdvisors(final List<MethodInterceptor> methodInterceptors, final List<Advisor> advisors)
    {
        final ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.setProxyClassLoader(this.getClass().getClassLoader());
        proxyFactoryBean.setTargetSource(new SingletonTargetSource(this));
        proxyFactoryBean.addAdvisors(advisors);
        methodInterceptors.forEach(proxyFactoryBean::addAdvice);
        return proxyFactoryBean.getObject();
    }

    private void detectAndRegisterRequestHandlerMethods(final Class<?> clazz, final Object object)
    {
        Arrays.asList(clazz.getMethods())
                .forEach(m -> {
                    final RequestMappingInfo mapping = getMappingForMethod(m, this.getClass());
                    if (mapping != null)
                    {
                        final String rootContextPath = context.getProjectConfiguration().getRootContextPath();
                        RequestMappingInfo mappingToUse = RequestMappingInfo.paths(rootContextPath).build();

                        if (context.getProjectConfiguration().enableUrlProjectContextPrefix())
                        {
                            final String projectContextPath = context.getProjectConfiguration().getProjectContextPath();
                            mappingToUse = mappingToUse.combine(RequestMappingInfo.paths(projectContextPath).build());
                        }
                        mappingToUse = mappingToUse.combine(mapping);
                        unregisterMapping(mappingToUse);
                        registerMapping(mappingToUse, object, m);
                    }
                });
    }

    public SpringMvcServerFunction()
    {
        for (Method m : ReflectionUtils.getUniqueDeclaredMethods(getClass()))
        {
            final RequestMappingInfo mapping = getMappingForMethod(m, this.getClass());
            if (mapping != null)
            {
                logger.info("Register mapping {}", mapping);
                registerMapping(mapping, this, m);
            }
        }
    }

    @Override
    public FunctionResult handle(final HttpRequest httpRequest, final HttpResponse httpResponse) throws Exception
    {
        final HttpServletRequest rawRequest = (HttpServletRequest) httpRequest.raw();
        final HttpServletResponse rawResponse = (HttpServletResponse) httpResponse.raw();
        final HandlerExecutionChain handler = getHandler(rawRequest);
        if (handler == null)
        {
            return FunctionResult.SKIPPED;
        }
        adapter.handle(rawRequest, rawResponse, handler.getHandler());
        return FunctionResult.PROCESSED;
    }

    @Override
    public void setContext(final FunctionContext context)
    {
        this.context = context;
    }

    public FunctionContext getContext()
    {
        return context;
    }
}
