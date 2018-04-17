package com.ethlo.lamebda.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.AntPathMatcher;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseServletServerFunction implements ServletServerFunction
{
    protected final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String pattern;
    
    public BaseServletServerFunction(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public boolean match(HttpServletRequest request)
    {
        return pathMatcher.match(pattern, request.getRequestURI()); 
    }
    
    protected void respond(HttpServletResponse response, int status, Object body)
    {
        try
        {
            objectMapper.writeValue(response.getOutputStream(), body);
        }
        catch (IOException exc)
        {
            throw new DataAccessResourceFailureException("Cannot serialize data", exc);
        }
    }
}
