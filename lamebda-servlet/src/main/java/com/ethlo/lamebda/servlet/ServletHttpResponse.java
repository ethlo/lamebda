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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.error.ErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ServletHttpResponse implements HttpResponse
{
    private static final ObjectMapper OM = new ObjectMapper();
    static
    {
        OM.setSerializationInclusion(Include.NON_NULL);
        OM.registerModule(new JavaTimeModule());
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    private final HttpServletResponse response;
    
    public ServletHttpResponse(HttpServletResponse response)
    {
        this.response = response;
    }

    @Override
    public void setStatus(int status)
    {
        response.setStatus(status);
        
    }

    @Override
    public void setContentType(String type)
    {
        response.setContentType(type);
    }

    @Override
    public void setCharacterEncoding(String charset)
    {
        response.setCharacterEncoding(charset);
    }

    @Override
    public void write(String body)
    {
        try (final Writer w = new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding()))
        {
            w.write(body);
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    @Override
    public void write(byte[] body)
    {
        try (final OutputStream out = response.getOutputStream())
        {
            out.write(body);
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc.getMessage(), exc);
        }
    }
    
    @Override
    public void error(ErrorResponse error)
    {
        json(error.getStatus(), error);
    }
    
    @Override
    public void error(int status)
    {
        json(status, new ErrorResponse(status));
    }
    
    @Override
    public void error(int status, String message)
    {
        json(status, new ErrorResponse(status, message));
    }
    
    @Override
    public void json(int status, Object body)
    {
        try (final OutputStream out = response.getOutputStream())
        {
            response.setStatus(status);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            OM.writeValue(out, body);
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    @Override
    public Object raw()
    {
        return response;
    }
}
