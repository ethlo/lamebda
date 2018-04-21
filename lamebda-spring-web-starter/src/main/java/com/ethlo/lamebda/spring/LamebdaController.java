package com.ethlo.lamebda.spring;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ethlo.lamebda.FunctionManager;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.error.ClientException;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.servlet.ServletHttpRequest;
import com.ethlo.lamebda.servlet.ServletHttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LamebdaController
{
    private FunctionManager functionManager;
    private String requestPath;
    private ObjectMapper mapper;
    
    public LamebdaController(FunctionManager functionManager, String requestPath, ObjectMapper mapper)
    {
        this.functionManager = functionManager;
        this.requestPath = requestPath;
        this.mapper = mapper;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response)
    {
        final String contextPath = request.getContextPath();
        final HttpRequest req = new ServletHttpRequest(contextPath + requestPath, request);
        final HttpResponse res = new ServletHttpResponse(response);
        try
        {
            functionManager.handle(req, res);
        }
        catch (ClientException exc)
        {
            final int status = HttpStatus.BAD_REQUEST;
            response.setStatus(status);
            try (final OutputStream out = response.getOutputStream())
            {
                mapper.writeValue(out, new ErrorResponse(status, exc.getMessage()));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }            
        }
    }
}