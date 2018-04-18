package com.ethlo.lamebda;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

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

public abstract class AntMappedServerFunction implements ServerFunction
{
    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();
    private final String pattern;

    public AntMappedServerFunction(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public boolean handle(HttpRequest request, HttpResponse response)
    {
        if (! PATH_MATCHER.match(pattern, request.path()))
        {
            return false;
        }
        
        doHandle(request, response);
        return true;
    }
    
    protected abstract void doHandle(HttpRequest request, HttpResponse response);
}
