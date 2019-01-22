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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import org.springframework.util.MimeType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.ethlo.lamebda.BaseServerFunction;
import com.ethlo.lamebda.FunctionResult;
import com.ethlo.lamebda.HttpMethod;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.URLMappedServerFunction;
import com.ethlo.lamebda.mapping.RequestMapping;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.reporting.MethodAndPattern;
import com.ethlo.lamebda.servlet.LamebdaMetricsFilter;

public class SpringMvcServerFunction extends BaseServerFunction implements URLMappedServerFunction
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenRequestMappingHandlerMapping openRequestMappingHandlerMapping = new OpenRequestMappingHandlerMapping();
    private final Set<RequestMapping> requestMappings = new LinkedHashSet<>();

    @Autowired
    private RequestMappingHandlerAdapter adapter;


    @Autowired(required = false)
    protected final void postConstruct(final ListableBeanFactory beanFactory, final List<MethodInterceptor> methodInterceptors)
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
                    final RequestMappingInfo mapping = openRequestMappingHandlerMapping.getMappingForMethod(m, this.getClass());
                    doRegister(object, m, mapping);
                });
    }

    private void doRegister(final Object object, final Method m, final RequestMappingInfo mapping)
    {
        if (mapping != null)
        {
            final String rootContextPath = context.getProjectConfiguration().getRootContextPath();
            RequestMappingInfo mappingToUse = RequestMappingInfo.paths(rootContextPath).build();

            if (context.getProjectConfiguration().enableUrlProjectContextPrefix())
            {
                final String projectContextPath = context.getProjectConfiguration().getContextPath();
                mappingToUse = mappingToUse.combine(RequestMappingInfo.paths(projectContextPath).build());
            }
            mappingToUse = mappingToUse.combine(mapping);
            final Set<HttpMethod> methods = mappingToUse.getMethodsCondition().getMethods().stream().map(method -> HttpMethod.parse(method.name())).collect(Collectors.toSet());
            final Set<String> patterns = mappingToUse.getPatternsCondition().getPatterns();
            final Set<String> consumes = mappingToUse.getConsumesCondition().getConsumableMediaTypes().stream().map(MimeType::toString).collect(Collectors.toSet());
            final Set<String> produces = mappingToUse.getProducesCondition().getProducibleMediaTypes().stream().map(MimeType::toString).collect(Collectors.toSet());
            this.requestMappings.add(new RequestMapping(patterns, methods, consumes, produces));
            openRequestMappingHandlerMapping.unregisterMapping(mappingToUse);
            openRequestMappingHandlerMapping.registerMapping(mappingToUse, object, m);
        }
    }

    @Override
    public FunctionResult handle(final HttpRequest httpRequest, final HttpResponse httpResponse) throws Exception
    {
        final HttpServletRequest rawRequest = (HttpServletRequest) httpRequest.raw();
        final HttpServletResponse rawResponse = (HttpServletResponse) httpResponse.raw();

        final HandlerExecutionChain handler = openRequestMappingHandlerMapping.getHandler(rawRequest);
        if (handler == null)
        {
            return FunctionResult.SKIPPED;
        }

        final String pattern = (String) rawRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        rawRequest.setAttribute(LamebdaMetricsFilter.PATTERN_ATTRIBUTE_NAME, pattern);

        try
        {
            adapter.handle(rawRequest, rawResponse, handler.getHandler());
            return FunctionResult.PROCESSED;
        }
        catch (Exception exc)
        {
            FunctionMetricsService.getInstance().errorOccured(new MethodAndPattern(rawRequest.getMethod(), pattern), exc);
            throw exc;
        }
    }

    @Override
    protected final void initInternal(FunctionContext context)
    {
        for (Method method : ReflectionUtils.getUniqueDeclaredMethods(getClass()))
        {
            final RequestMappingInfo mapping = openRequestMappingHandlerMapping.getMappingForMethod(method, this.getClass());
            doRegister(this, method, mapping);
        }
    }

    public FunctionContext getContext()
    {
        return context;
    }

    @Override
    public Set<RequestMapping> getUrlMapping()
    {
        return new TreeSet<>(requestMappings);
    }
}
