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

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.SimpleServerFunction;

public class SingleResourceFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private final byte[] content;
    private final String mimeType;

    public SingleResourceFunction(String urlPath, String mimeType, byte[] content)
    {
        super(urlPath);
        this.mimeType = mimeType;
        this.content = content;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response)
    {
        response.setContentType(mimeType);
        response.write(content);
    }
}
