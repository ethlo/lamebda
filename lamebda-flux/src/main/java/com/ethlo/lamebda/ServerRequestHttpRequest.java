package com.ethlo.lamebda;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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
        final Multimap<String, String> map = ArrayListMultimap.create();
        final HttpHeaders headers = request.headers().asHttpHeaders();
        for (Entry<String, List<String>> e : headers.entrySet())
        {
            map.putAll(e.getKey(), e.getValue());    
        }
        return map;
    }

    @Override
    public Multimap<String, String> queryParams()
    {
        final Multimap<String, String> map = ArrayListMultimap.create();
        for (Entry<String, List<String>> e : request.queryParams().entrySet())
        {
            map.putAll(e.getKey(), e.getValue());    
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

}
