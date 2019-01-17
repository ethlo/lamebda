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

import org.junit.Test;

import com.ethlo.lamebda.test.MockHttpRequest;
import com.ethlo.lamebda.test.MockHttpResponse;

public class ServerFunctionTest extends BaseTest
{
    @Test
    public void testServingStaticResource() throws Exception
    {
        final MockHttpRequest req = new MockHttpRequest();
        final MockHttpResponse res = new MockHttpResponse();
        req.path("/static/incorrect.html");
        req.method("GET");
        functionManager.handle(req, res);
        assertThat(res.body()).contains("incorrect.html");
    }
}
