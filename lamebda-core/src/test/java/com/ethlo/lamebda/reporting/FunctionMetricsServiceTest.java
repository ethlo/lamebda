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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.Test;

import com.ethlo.lamebda.HttpMethod;
import com.ethlo.lamebda.HttpStatus;

public class FunctionMetricsServiceTest
{
    private final FunctionMetricsService metricsService = FunctionMetricsService.getInstance();

    private final String pattern = "/foo/bar/{baz}";

    @Test
    public void requestHandled()
    {
        metricsService.clear();
        final MethodAndPattern requestMapping = invoke();

        for (int i = 0; i < 100; i++)
        {
            invoke();
        }

        final Map<Integer, Long> perPattern = metricsService.getMetrics(requestMapping);
        assertThat(perPattern.get(HttpStatus.OK)).isEqualTo(101);
    }

    private MethodAndPattern invoke()
    {
        final MethodAndPattern requestMapping = new MethodAndPattern(HttpMethod.POST.toString(), pattern);
        final int millis = (int) (Math.random() * 1000);
        metricsService.requestHandled("smith", OffsetDateTime.now(), requestMapping, Duration.ofMillis(millis), HttpStatus.OK);
        return requestMapping;
    }
}
