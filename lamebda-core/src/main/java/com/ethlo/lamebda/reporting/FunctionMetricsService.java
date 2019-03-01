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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.time.ITU;

public class FunctionMetricsService
{
    private static final Logger performanceLogger = LoggerFactory.getLogger("lamebda.performance");

    private final ConcurrentMap<MethodAndPattern, ConcurrentMap<Integer, Long>> invocations = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodAndPattern, OffsetDateTime> firstInvocation = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodAndPattern, OffsetDateTime> lastInvocation = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodAndPattern, AtomicLong> totalRuntime = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodAndPattern, String> lastErrors = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodAndPattern, Double> lastResponseTimes = new ConcurrentHashMap<>();

    private static final FunctionMetricsService INSTANCE = new FunctionMetricsService();

    public static FunctionMetricsService getInstance()
    {
        return INSTANCE;
    }

    private FunctionMetricsService()
    {
    }

    public void errorOccured(MethodAndPattern pattern, Exception exc)
    {
        lastErrors.put(pattern, ITU.formatUtc(OffsetDateTime.now()) + ": " + exc.getClass().getCanonicalName() + " - " + exc.getMessage());
    }

    public void requestHandled(String userId, OffsetDateTime timestamp, MethodAndPattern mapping, Duration elapsedTime, int httpStatus)
    {
        final String minimalMapping = mapping.getMethod() + " " + mapping.getPattern();
        performanceLogger.info("{} - {} - {} - {} - {}", ITU.formatUtcMilli(timestamp), userId, minimalMapping, elapsedTime.toMillis(), httpStatus);

        invocations.compute(mapping, (k, perMapping) -> {
            if (perMapping == null)
            {
                perMapping = new ConcurrentHashMap<>();
            }
            perMapping.compute(httpStatus, (ki, count) -> count != null ? count + 1 : 1);
            return perMapping;
        });

        totalRuntime.compute(mapping, (k, v) -> {
            if (v != null)
            {
                v.addAndGet(elapsedTime.toMillis());
            }
            else
            {
                v = new AtomicLong(elapsedTime.toMillis());
            }
            return v;
        });

        final OffsetDateTime now = OffsetDateTime.now();
        firstInvocation.putIfAbsent(mapping, now);
        lastInvocation.compute(mapping, (k, v) -> now);
        lastResponseTimes.put(mapping, elapsedTime.toNanos() / 1_000_000D);
    }

    public Map<Integer, Long> getMetrics(final MethodAndPattern requestMapping)
    {
        return invocations.get(requestMapping);
    }

    public Map<MethodAndPattern, FunctionMetric> getMetrics()
    {
        final Map<MethodAndPattern, FunctionMetric> retVal = new LinkedHashMap<>();
        for (final Map.Entry<MethodAndPattern, ConcurrentMap<Integer, Long>> e : invocations.entrySet())
        {
            final MethodAndPattern mapping = e.getKey();
            final AtomicLong totalElapsed = totalRuntime.getOrDefault(mapping, new AtomicLong(1));
            final double lastResponseTime = lastResponseTimes.get(mapping);
            final double avg = (totalElapsed.doubleValue() / e.getValue().values().stream().mapToDouble(d -> d).sum());
            final String lastError = lastErrors.get(mapping);
            retVal.put(mapping, new FunctionMetric(firstInvocation.get(mapping), e.getValue(), lastInvocation.get(mapping), avg, lastError, lastResponseTime));
        }

        return new TreeMap<>(retVal);
    }

    public void clear()
    {
        this.invocations.clear();
        this.lastResponseTimes.clear();
        this.firstInvocation.clear();
        this.lastInvocation.clear();
        this.lastErrors.clear();
        this.totalRuntime.clear();
    }

    private class FunctionMetric
    {
        private final OffsetDateTime firstInvocation;
        private final Map<Integer, Long> countByStatus;
        private final OffsetDateTime lastInvocation;
        private final double averageResponseTime;
        private final String lastError;
        private final double lastResponseTime;

        public FunctionMetric(final OffsetDateTime firstInvocation, final Map<Integer, Long> countByStatus, final OffsetDateTime lastInvocation, final double averageResponseTime, String lastError, double lastResponseTime)
        {
            this.firstInvocation = firstInvocation;
            this.countByStatus = countByStatus;
            this.lastInvocation = lastInvocation;
            this.averageResponseTime = averageResponseTime;
            this.lastError = lastError;
            this.lastResponseTime = lastResponseTime;
        }

        public OffsetDateTime getFirstInvocation()
        {
            return firstInvocation;
        }

        public Map<Integer, Long> getCountByStatus()
        {
            return countByStatus;
        }

        public OffsetDateTime getLastInvocation()
        {
            return lastInvocation;
        }

        public double getAverageResponseTime()
        {
            return averageResponseTime;
        }

        public double getLastResponseTime()
        {
            return lastResponseTime;
        }

        public String getLastError()
        {
            return lastError;
        }
    }
}
