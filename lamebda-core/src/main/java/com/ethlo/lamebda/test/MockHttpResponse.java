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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.util.Multimap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MockHttpResponse implements HttpResponse
{
    private static final ObjectMapper OM = new ObjectMapper();

    static
    {
        OM.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private int status;
    private String contentType;
    private String characterEncoding = StandardCharsets.UTF_8.displayName();
    private ByteArrayOutputStream body = new ByteArrayOutputStream();
    private Multimap<String,String> headers;

    @Override
    public void setStatus(final int status)
    {
        this.status = status;
    }

    @Override
    public void setContentType(final String contentType)
    {
        this.contentType = contentType;
    }

    @Override
    public void setCharacterEncoding(final String characterEncoding)
    {
        this.characterEncoding = characterEncoding;
    }

    @Override
    public void write(final String body)
    {
        try
        {
            this.body.write(body.getBytes(characterEncoding));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(final byte[] body)
    {
        try
        {
            this.body.write(body);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
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
        try (final OutputStream out = this.body)
        {
            this.status = status;
            this.contentType = "application/json";
            this.characterEncoding = StandardCharsets.UTF_8.name();
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
        return this;
    }

    @Override
    public void addHeader(final String name, final String value)
    {
        this.headers.add(name, value);
    }

    public byte[] rawBody()
    {
        return this.body.toByteArray();
    }

    public String body()
    {
        try
        {
            return new String(this.rawBody(), characterEncoding);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
