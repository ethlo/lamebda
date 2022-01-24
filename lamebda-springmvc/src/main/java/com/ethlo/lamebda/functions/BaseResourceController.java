package com.ethlo.lamebda.functions;

/*-
 * #%L
 * lamebda-springmvc
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;

import com.ethlo.lamebda.util.IoUtil;

public abstract class BaseResourceController
{
    private final Map<String, String> extensionMappings = new TreeMap<>();

    public BaseResourceController()
    {
        extensionMappings.put("html", "text/html");
        extensionMappings.put("css", "text/css");
        extensionMappings.put("js", "application/javascript");
        extensionMappings.put("gif", "image/gif");
        extensionMappings.put("png", "image/png");
    }

    @GetMapping(value = "/**")
    public void serve(final HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        final AntPathMatcher apm = new AntPathMatcher();
        final String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

        final Optional<Resource> resource = findResource(finalPath);

        if (resource.isPresent() && resource.get().exists())
        {
            try (final InputStream in = resource.get().getInputStream(); final OutputStream out = response.getOutputStream())
            {
                final Optional<String> extOpt = IoUtil.getExtension(path);
                extOpt.ifPresent(ext -> response.setHeader("Content-Type", extensionMappings.get(ext)));
                IoUtil.copy(in, out);
            }
        }
        else
        {
            response.sendError(HttpStatus.NOT_FOUND.value());
        }
    }

    protected abstract Optional<Resource> findResource(final String finalPath);
}
