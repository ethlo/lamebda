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

public class HttpMimeType
{
    private HttpMimeType(){}
    
    public static final String HTML = "text/html";
    public static final String JSON = "application/json";
    public static final String JAVASCRIPT = "application/javascript";
    public static final String CSV = "application/csv";
    public static final String TEXT = "text/plain";
    public static final String XML = "application/xml";
    public static final String YAML = "text/vnd.yaml";
    public static final String JPG = "image/jpg";
    public static final String PNG = "image/png";
    public static final String GIF = "image/gif";

    public static final String APPLICATION = "application/octet-stream";

    public static String fromExtension(String extension)
    {
        switch (extension.toLowerCase())
        {
            case "html":
                return HTML;

            case "json":
                    return JSON;

            case "js":
                return JAVASCRIPT;

            case "csv":
                return CSV;

            case "txt":
                return TEXT;

            case "xml":
                return XML;

            case "yaml":
                return YAML;

            case "jpg":
                return JPG;

            case "png":
                return PNG;

            case "gif":
                return GIF;

            default:
                return APPLICATION;
        }
    }
}
