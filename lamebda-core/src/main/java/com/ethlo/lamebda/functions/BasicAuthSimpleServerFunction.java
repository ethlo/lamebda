package com.ethlo.lamebda.functions;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.SimpleServerFunction;

public abstract class BasicAuthSimpleServerFunction extends SimpleServerFunction
{
    protected BasicAuthSimpleServerFunction(String pattern)
    {
        super(pattern);
    }

    @Override
    protected void doHandle(final HttpRequest request, final HttpResponse response) throws IOException
    {
        final List<String> authHeader = request.header("Authorization");
        if (authHeader == null || authHeader.size() != 1)
        {
            commence(response);
            return;
        }

        final String value = authHeader.get(0);
        final String[] parts = value.split(" ");
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("Basic"))
        {
            commence(response);
            return;
        }

        final String userPassB64 = parts[1];
        final String clearToken = new String(Base64.getDecoder().decode(userPassB64), StandardCharsets.UTF_8);
        final String usernamePasswordParts[] = clearToken.split(":");

        if (usernamePasswordParts.length != 2)
        {
            commence(response);
            return;
        }

        final String username = usernamePasswordParts[0];
        final String password = usernamePasswordParts[1];

        if (! allow(username, password))
        {
            commence(response);
            return;
        }

        super.doHandle(request, response);
    }

    private void commence(final HttpResponse response)
    {
        response.addHeader("WWW-Authenticate", "Basic realm=\"Lamebda\"");
        response.error(401, "Unauthorized");
    }

    protected abstract boolean allow(String username, String password);
}
