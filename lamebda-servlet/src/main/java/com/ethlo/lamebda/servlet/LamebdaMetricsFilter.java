package com.ethlo.lamebda.servlet;

/*-
 * #%L
 * lamebda-servlet
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
import java.security.Principal;
import java.time.Duration;
import java.time.OffsetDateTime;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.reporting.HttpStatusWithReason;
import com.ethlo.lamebda.reporting.MethodAndPattern;

public class LamebdaMetricsFilter implements Filter
{
    public static final String REASON_ATTRIBUTE_NAME = "_lamebda_status_reason";
    public static final String PATTERN_ATTRIBUTE_NAME = "lamebda_pattern";

    private final FunctionMetricsService functionMetricsService;

    public LamebdaMetricsFilter(FunctionMetricsService functionMetricsService)
    {
        this.functionMetricsService = functionMetricsService;
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException
    {
        final long startedNanos = System.nanoTime();
        final OffsetDateTime startedTimestamp = OffsetDateTime.now();

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        try
        {
            chain.doFilter(request, response);
        }
        finally
        {
            final String pattern = (String) request.getAttribute(LamebdaMetricsFilter.PATTERN_ATTRIBUTE_NAME);
            if (pattern != null)
            {
                final String reason = (String) request.getAttribute(REASON_ATTRIBUTE_NAME);

                final MethodAndPattern requestMapping = new MethodAndPattern(request.getMethod(), pattern);
                logInvocation(requestMapping, request, startedTimestamp, startedNanos, System.nanoTime(), new HttpStatusWithReason(response.getStatus(), reason != null ? reason : ""));
            }
        }
    }

    private void logInvocation(MethodAndPattern requestMapping, HttpServletRequest request, final OffsetDateTime started, long startedNanos, long endedNanos, final HttpStatusWithReason httpStatusWithReason)
    {
        final Principal principal = request.getUserPrincipal();
        final String username = principal != null ? principal.getName() : "";
        final Duration duration = Duration.ofNanos(endedNanos - startedNanos);
        functionMetricsService.requestHandled(username, started, requestMapping, duration, httpStatusWithReason);
    }

    @Override
    public void destroy()
    {

    }

    @Override public void init(final FilterConfig filterConfig) throws ServletException
    {

    }
}
