package com.ethlo.lamebda;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;

import com.ethlo.lamebda.util.Multimap;

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

public interface HttpRequest
{
    String method();
    
    Multimap<String, String> headers();
    
    Multimap<String, String> queryParams();
    
    Object raw();
    
    /**
     * Case insensitive
     * @param name
     * @return
     */
    List<String> header(String name);
    
    /**
     * Case sensitive. Returns error on missing parameter. See also {@link #param(String, String)}
     * @param name
     * @return
     */
    String param(String name);
    
    /**
     * Case sensitive
     * @param name
     * @return
     */
    String param(String name, String defaultValue);
    
    /**
     * Returns the request path
     * @return
     */
    String path();
    
    /**
     * Returns the raw body as a byte array
     * @return
     */
    byte[] rawBody();
    
    /**
     * Returns the body as a UTF-8 encoded string
     * @return
     */
    String body();
    
    /**
     * Returns the body as a JSON object
     * @return
     */
    Object json();
    
    /**
     * Returns the remote host calling this function
     * @return
     */
    String remoteIp();
    
    /**
     * Returns the remote host calling this function
     * @return
     */
    InetAddress remoteIpAddress();

    /**
     * 
     * @return
     */
    Charset charset();

    String contentType();
}
