package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-flux
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.ServerRequest;

import com.ethlo.lamebda.util.Multimap;

public class ServerRequestHttpRequest implements HttpRequest
{
    private final ServerRequest request;
    
    public ServerRequestHttpRequest(ServerRequest request)
    {
        this.request = request;
    }
    
    @Override
    public String method()
    {
        return request.methodName();
    }

    @Override
    public Multimap<String, String> headers()
    {
        final Multimap<String, String> map = new Multimap<>();
        final HttpHeaders headers = request.headers().asHttpHeaders();
        for (Entry<String, List<String>> e : headers.entrySet())
        {
            map.addAll(e.getKey(), e.getValue());    
        }
        return map;
    }

    @Override
    public Multimap<String, String> queryParams()
    {
        final Multimap<String, String> map = new Multimap<>();
        for (Entry<String, List<String>> e : request.queryParams().entrySet())
        {
            map.addAll(e.getKey(), e.getValue());    
        }
        return map;
    }

    @Override
    public Object raw()
    {
        return request;
    }

    @Override
    public List<String> header(String name)
    {
        return request.headers().header(name);
    }

    @Override
    public String param(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String path()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] rawBody()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String body()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object json()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String remoteIp()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String param(String name, String defaultValue)
    {
        final Optional<String> value = request.queryParam(name);
        if (value.isPresent())
        {
            return value.get();
        }
        return defaultValue;
    }

    @Override
    public InetAddress remoteIpAddress()
    {
        try
        {
            return InetAddress.getByName(remoteIp());
        }
        catch (UnknownHostException exc)
        {
            throw new IllegalArgumentException("Cannot parse IP address: " + this.remoteIp(), exc);
        }
    }

    @Override
    public Charset charset()
    {
        final List<Charset> charsets = request.headers().acceptCharset();
        return !charsets.isEmpty() ? charsets.get(0) : StandardCharsets.UTF_8;
    }
}
