package com.ethlo.lamebda;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.mapping.RequestMapping;
import com.ethlo.lamebda.util.StringUtil;

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

public abstract class SimpleServerFunction extends BaseServerFunction implements URLMappedServerFunction
{
    private static final Logger logger = LoggerFactory.getLogger(SimpleServerFunction.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    protected String pattern;

    public SimpleServerFunction()
    {
        pattern = StringUtil.camelCaseToHyphen(this.getClass().getName());
    }

    public SimpleServerFunction(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public final FunctionResult handle(HttpRequest request, HttpResponse response) throws Exception
    {
        final String path = request.path();
        if (! isMatch(path))
        {
            logger.debug("Function {}. Request path {} does NOT match pattern {}", this.getClass().getSimpleName(), path, pattern);
            return FunctionResult.SKIPPED;
        }
        logger.debug("Function {}. Handling request with path {}", this.getClass().getSimpleName(), path);
        doHandle(request, response);
        return FunctionResult.PROCESSED;
    }

    private boolean isMatch(final String path)
    {
        if (path.equals(pattern))
        {
            // Special matching for root
            return true;
        }
        return PATH_MATCHER.match(pattern, path);
    }

    protected void doHandle(HttpRequest request, HttpResponse response) throws IOException
    {
        final HttpMethod m = HttpMethod.parse(request.method());
        if (m == null)
        {
            response.error(new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Cannot handle HTTP method: " + request.method()));
            return;
        }

        switch (m)
        {
            case POST:
                post(request, response);
                break;
            case CONNECT:
                connect(request, response);
                break;
            case DELETE:
                delete(request, response);
                break;
            case GET:
                get(request, response);
                break;
            case HEAD:
                head(request, response);
                break;
            case OPTIONS:
                options(request, response);
                break;
            case PATCH:
                patch(request, response);
                break;
            case PUT:
                put(request, response);
                break;
            case TRACE:
                trace(request, response);
                break;
        }
    }

    protected void post(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected void get(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected void connect(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected void put(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected void delete(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected void head(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    private ErrorResponse getErrorResponse(final String method)
    {
        return new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method " + method + " not allowed");
    }

    protected void options(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected void patch(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected void trace(HttpRequest request, HttpResponse response)
    {
        response.error(getErrorResponse(request.method()));
    }

    protected Map<String, String> getPathVars(String pattern, HttpRequest request)
    {
        try
        {
            return PATH_MATCHER.extractUriTemplateVariables(pattern, request.path());
        }
        catch (IllegalStateException exc)
        {
            return Collections.emptyMap();
        }
    }

    @Override
    public final void initInternal(FunctionContext context)
    {
        final ProjectConfiguration cfg = context.getProjectConfiguration();
        final String projectContext = (cfg.enableUrlProjectContextPrefix() ? (cfg.getContextPath() + "/") : "");
        this.pattern = StringUtils.stripEnd(URI.create("/" + projectContext + pattern).normalize().toString(), "/");
    }

    public FunctionContext getContext()
    {
        return context;
    }

    @Override
    public Set<RequestMapping> getUrlMapping()
    {
        final ProjectConfiguration cfg = context.getProjectConfiguration();
        final URI url = URI.create(cfg.getRootContextPath() + "/" + pattern);
        return Collections.singleton(new RequestMapping(Collections.singleton(url.normalize().toString()), Collections.emptySet(), null, null));
    }
}
