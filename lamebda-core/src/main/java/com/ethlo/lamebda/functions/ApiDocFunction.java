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

import java.nio.charset.StandardCharsets;

import com.ethlo.lamebda.HttpMimeType;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.util.IoUtil;

public class ApiDocFunction extends SimpleServerFunction
{
    public ApiDocFunction()
    {
        super("/doc/*");
    }

    @Override
    protected void get(HttpRequest request, HttpResponse response)
    {
        final String docPage = IoUtil.classPathResourceAsString("doc.html", StandardCharsets.UTF_8);
        response.setContentType(HttpMimeType.HTML);
        response.write(docPage);
    }
}
