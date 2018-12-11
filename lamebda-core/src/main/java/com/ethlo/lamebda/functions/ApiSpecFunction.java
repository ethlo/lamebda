package com.ethlo.lamebda.functions;

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

import java.util.Map;

import com.ethlo.lamebda.ApiSpecLoader;
import com.ethlo.lamebda.ClassResourceLoader;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.util.StringUtil;

public class ApiSpecFunction extends SimpleServerFunction
{
    private final Map<String, ServerFunction> functions;
    private final ApiSpecLoader loader;

    public ApiSpecFunction(Map<String, ServerFunction> functions, ApiSpecLoader loader)
    {
        super("/doc/*.json");
        this.functions = functions;
        this.loader = loader;
    }

    @Override
    protected void get(HttpRequest request, HttpResponse response)
    {
        final String name = getPathVars("/doc/{function}.*", request).get("function");
        final String functionName = StringUtil.hyphenToCamelCase(name);
        final boolean functionExists = functions.containsKey(functionName);
        if (functionExists)
        {
            response.write(loader.loadApiSpec(functionName));
        }
        else
        {
            response.error(HttpStatus.NOT_FOUND, "No API documentation file was found");
        }
    }
}
