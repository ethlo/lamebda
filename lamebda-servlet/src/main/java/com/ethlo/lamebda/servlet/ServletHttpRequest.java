package com.ethlo.lamebda.servlet;

/*-
 * #%L
 * lamebda-servlet
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.error.InvalidJsonException;
import com.ethlo.lamebda.error.MissingRequestParamException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;

import groovy.json.JsonException;
import groovy.json.JsonSlurper;

public class ServletHttpRequest implements HttpRequest
{
    private static final JsonSlurper JSON_SLURPER = new JsonSlurper();
    
    private final HttpServletRequest request;
    
    private byte[] body;
    private Multimap<String, String> queryParams;
    private String path;

    public ServletHttpRequest(String prefix, HttpServletRequest request)
    {
        Assert.notNull(request, "request cannot be null");
        this.path = request.getRequestURI().substring(request.getContextPath().length()).substring(prefix.length());
        this.request = request;
    }
    
    @Override
    public List<String> header(String name)
    {
        return new ArrayList<>(headers().get(name));
    }

    @Override
    public String param(String name)
    {
        final String res = request.getParameter(name);
        if (res != null)
        {
            return res; 
        }
        throw new MissingRequestParamException(name);
    }
    
    @Override
    public String param(String name, String defaultValue)
    {
        final String res = request.getParameter(name);
        if (res != null)
        {
            return res; 
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
        if (body == null)
        {
            try
            {
                body = ByteStreams.toByteArray(request.getInputStream());
            }
            catch (IOException exc)
            {
                throw new DataAccessResourceFailureException("Cannot read body contents", exc);
            }
        }
        return body;
    }

    @Override
    public String body()
    {
        try
        {
            return new String(rawBody(), request.getCharacterEncoding());
        }
        catch (UnsupportedEncodingException exc)
        {
            throw new DataAccessResourceFailureException("Cannot read body contents as the encoding is not supported", exc);
        }
    }

    @Override
    public Object json()
    {
        try
        {
            return JSON_SLURPER.parse(rawBody());
        }
        catch (JsonException exc)
        {
            throw new InvalidJsonException(exc.getMessage(), exc);
        }
    }

    @Override
    public String remoteIp()
    {
        return request.getRemoteAddr();
    }

    @Override
    public Multimap<String, String> headers()
    {
        final Multimap<String, String> map = ArrayListMultimap.create();
        final Enumeration<String> names = request.getHeaderNames();
        while(names.hasMoreElements())
        {
            final String name = names.nextElement();
            map.putAll(name, Collections.list(request.getHeaders(name)));
        }
        return map;
    }

    @Override
    public Multimap<String, String> queryParams()
    {
        if (queryParams == null)
        {
            queryParams = ArrayListMultimap.create();
            for (Entry<String, String[]> e : request.getParameterMap().entrySet())
            {
                queryParams.putAll(e.getKey(), Arrays.asList(e.getValue()));
            }
        }
        return queryParams;
    }

    @Override
    public Object raw()
    {
        return request;
    }

    @Override
    public String method()
    {
        return request.getMethod();
    }

    @Override
    public InetAddress remoteIpAddress()
    {
        try
        {
            return InetAddress.getByName(this.remoteIp());
        }
        catch (UnknownHostException exc)
        {
            throw new IllegalArgumentException("Cannot parse IP address: " + this.remoteIp(), exc);
        }
    }
}
