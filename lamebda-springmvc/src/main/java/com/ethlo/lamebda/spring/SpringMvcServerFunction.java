package com.ethlo.lamebda.spring;

/*-
 * #%L
 * lamebda-spring-web-starter
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

import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.FunctionResult;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.URLMappedServerFunction;
import com.ethlo.lamebda.mapping.RequestMapping;

@Deprecated
public abstract class SpringMvcServerFunction implements ServerFunction, URLMappedServerFunction
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public FunctionResult handle(final HttpServletRequest request, final HttpServletResponse response) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<RequestMapping> getUrlMapping()
    {
        return Collections.emptySet();
    }
}
