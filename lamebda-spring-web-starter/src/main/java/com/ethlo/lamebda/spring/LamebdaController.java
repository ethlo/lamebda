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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ethlo.lamebda.FunctionManager;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.servlet.ServletHttpRequest;
import com.ethlo.lamebda.servlet.ServletHttpResponse;

public class LamebdaController
{
    private FunctionManager functionManager;
    private String rootContextPath;

    public LamebdaController(FunctionManager functionManager, String rootContextPath)
    {
        this.functionManager = functionManager;
        this.rootContextPath = rootContextPath;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        final String contextPath = request.getContextPath();
        final HttpRequest req = new ServletHttpRequest(contextPath + this.rootContextPath, request);
        final HttpResponse res = new ServletHttpResponse(response);
        if (!functionManager.handle(req, res))
        {
            res.error(ErrorResponse.notFound("No function found to handle '" + req.path() + "'"));
        }
    }
}
