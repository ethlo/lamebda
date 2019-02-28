package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-core
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.ethlo.lamebda.functions.ProjectStatusFunction;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.reporting.MethodAndPattern;
import com.ethlo.lamebda.test.MockHttpRequest;
import com.ethlo.lamebda.test.MockHttpResponse;

public class ServerFunctionTest extends BaseTest
{
    public ServerFunctionTest() throws IOException
    {
    }

    @Test
    public void testServingStaticResource() throws Exception
    {
        final MockHttpRequest req = new MockHttpRequest();
        final MockHttpResponse res = new MockHttpResponse();
        req.path("/lamebda-unit-test/static/incorrect.html");
        req.method("GET");
        functionManager.handle(req, res);
        assertThat(res.body()).contains("incorrect.html");
    }

    @Test
    public void testProjectStatusInfo()
    {
        final FunctionMetricsService metricsService = FunctionMetricsService.getInstance();
        final ProjectStatusFunction projectStatusFunction = new ProjectStatusFunction("/status", functionManager, metricsService);

        final MethodAndPattern requestMapping = new MethodAndPattern(HttpMethod.POST.toString(), "/foo/bar/{baz}");
        final int millis = (int) (Math.random() * 1000);
        metricsService.requestHandled("smith", OffsetDateTime.now(), requestMapping, Duration.ofMillis(millis), HttpStatus.OK);

        final MockHttpResponse resp = new MockHttpResponse();
        projectStatusFunction.get(new MockHttpRequest().path("/status"), resp);
        assertThat(resp.status()).isEqualTo(HttpStatus.OK);
        assertThat(resp.body()).isNotNull();
    }
}
