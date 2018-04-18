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
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessResourceFailureException;

import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.error.HttpError;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServletHttpResponse implements HttpResponse
{
    private static final ObjectMapper OM = new ObjectMapper();
    
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
        try (final OutputStream out = response.getOutputStream())
        {
            new OutputStreamWriter(out).write(body);
        }
        catch (IOException exc)
        {
            throw new DataAccessResourceFailureException("Cannot send data", exc);
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
            throw new DataAccessResourceFailureException("Cannot send data", exc);
        }
    }
    
    @Override
    public void respond(HttpError error)
    {
        respondJson(error.getStatus(), error);
    }
    
    @Override
    public void respondError(int status)
    {
        respondJson(status, new HttpError(status));
    }
    
    @Override
    public void respondError(int status, String message)
    {
        respondJson(status, new HttpError(status, message));
    }
    
    @Override
    public void respondJson(int status, Object body)
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
            throw new DataAccessResourceFailureException("Cannot send data", exc);
        }
    }
}
