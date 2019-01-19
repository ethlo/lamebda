package com.ethlo.lamebda.reporting;

/*-
 * #%L
 * lamebda-core
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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.time.ITU;

public class FunctionMetricsService
{
    private static final Logger performanceLogger = LoggerFactory.getLogger("lamebda.performance");

    private final ConcurrentMap<MethodAndPattern, ConcurrentMap<HttpStatusWithReason, Long>> invocations = new ConcurrentHashMap<>();

    private static final FunctionMetricsService INSTANCE = new FunctionMetricsService();

    public static FunctionMetricsService getInstance()
    {
        return INSTANCE;
    }

    private FunctionMetricsService()
    {
    }

    public void requestHandled(String userId, OffsetDateTime timestamp, MethodAndPattern mapping, Duration elapsedTime, HttpStatusWithReason httpStatusWithReason)
    {
        final String minimalMapping = mapping.getMethod() + " " + mapping.getPattern();
        performanceLogger.info("{} - {} - {} - {} - {} {}", ITU.formatUtcMilli(timestamp), userId, minimalMapping, elapsedTime.toMillis(), httpStatusWithReason.getStatusCode(), httpStatusWithReason.getReasonPhrase());

        invocations.compute(mapping, (k, perMapping) -> {
            if (perMapping == null)
            {
                perMapping = new ConcurrentHashMap<>();
            }
            perMapping.compute(httpStatusWithReason, (ki, count) -> count != null ? count + 1 : 1);
            return perMapping;
        });
    }

    public Map<HttpStatusWithReason, Long> getMetrics(final MethodAndPattern requestMapping)
    {
        return invocations.get(requestMapping);
    }

    public ConcurrentMap<MethodAndPattern, ConcurrentMap<HttpStatusWithReason, Long>> getInvocations()
    {
        return this.invocations;
    }
}
