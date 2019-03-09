package com.ethlo.lamebda;

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

import org.springframework.util.Assert;

public enum HttpMethod
{
    /**
     * The GET method requests a representation of the specified resource. Requests using GET should only retrieve data.
     */
    GET,

    /**
     * The HEAD method asks for a response identical to that of a GET request, but without the response body.
     */
    HEAD,

    /**
     * The POST method is used to submit an entity to the specified resource, often causing a change in state or side effects on the server
     */
    POST,

    /**
     * The PUT method replaces all current representations of the target resource with the request payload.
     */
    PUT,

    /**
     * The DELETE method deletes the specified resource.
     */
    DELETE,

    /**
     * The CONNECT method establishes a tunnel to the server identified by the target resource.
     */
    CONNECT,

    /**
     * The OPTIONS method is used to describe the communication options for the target resource.
     */
    OPTIONS,

    /**
     * The TRACE method performs a message loop-back test along the path to the target resource.
     */
    TRACE,

    /**
     * The PATCH method is used to apply partial modifications to a resource.
     */
    PATCH;

    public static HttpMethod parse(String method)
    {
        Assert.notNull(method, "method cannot be null");
        try
        {
            return valueOf(method.toUpperCase());
        }
        catch (IllegalArgumentException exc)
        {
            return null;
        }
    }
}
