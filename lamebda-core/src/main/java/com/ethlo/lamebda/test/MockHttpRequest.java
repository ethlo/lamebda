package com.ethlo.lamebda.test;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.error.MissingRequestParamException;
import com.ethlo.lamebda.util.Multimap;
import groovy.json.JsonSlurper;

public class MockHttpRequest implements HttpRequest
{
    private static final JsonSlurper JSON_SLURPER = new JsonSlurper();

    private ByteArrayOutputStream body = new ByteArrayOutputStream(256);
    private String method = null;
    private Multimap<String, String> headers = new Multimap<>();
    private Multimap<String, String> queryParams = new Multimap<>();
    private String path;
    private InetAddress remoteIp;
    private Charset charset = StandardCharsets.UTF_8;
    private String contentType;

    @Override
    public String method()
    {
        return method;
    }

    @Override
    public Multimap<String, String> headers()
    {
        return headers;
    }

    @Override public Multimap<String, String> queryParams()
    {
        return queryParams;
    }

    @Override public Object raw()
    {
        return this;
    }

    @Override
    public List<String> header(final String name)
    {
        return new ArrayList<>(headers.get(name));
    }

    @Override
    public String param(final String name)
    {
        final Set<String> res = queryParams.get(name);
        if (res != null && !res.isEmpty())
        {
            return res.iterator().next();
        }
        throw new MissingRequestParamException(name);
    }

    @Override
    public String param(final String name, final String defaultValue)
    {
        final Set<String> res = queryParams.get(name);
        if (res != null && !res.isEmpty())
        {
            return res.iterator().next();
        }
        return defaultValue;
    }

    @Override
    public String path()
    {
        return path;
    }

    @Override
    public byte[] rawBody()
    {
        return body.toByteArray();
    }

    @Override
    public String body()
    {
        return new String(rawBody(), charset());
    }

    @Override
    public Object json()
    {
        return JSON_SLURPER.parse(rawBody());
    }

    @Override
    public String remoteIp()
    {
        return remoteIp.toString();
    }

    @Override
    public InetAddress remoteIpAddress()
    {
        return remoteIp;
    }

    @Override
    public Charset charset()
    {
        return charset;
    }

    @Override
    public String contentType()
    {
        return contentType;
    }

    public MockHttpRequest path(final String path)
    {
        this.path = path;
        return this;
    }

    public MockHttpRequest method(final String method)
    {
        this.method = method;
        return this;
    }

    public MockHttpRequest contentType(final String contentType)
    {
        this.contentType = contentType;
        return this;
    }

    public MockHttpRequest body(final byte[] bytes) throws IOException
    {
        this.body = new ByteArrayOutputStream();
        this.body.write(bytes);
        return this;
    }
}
