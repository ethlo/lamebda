package com.ethlo.lamebda.mapping;

/*-
 * #%L
 * lamebda-core
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

import java.util.Collections;
import java.util.Set;

import org.springframework.lang.NonNull;

import com.ethlo.lamebda.HttpMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

public record RequestMapping(Set<String> patterns, Set<HttpMethod> methods, Set<String> consumes,
                             Set<String> produces) implements Comparable<RequestMapping>
{

    public static RequestMapping of(final HttpMethod method, String pattern)
    {
        return new RequestMapping(Collections.singleton(pattern), Collections.singleton(method), null, null);
    }

    private static MethodAndPattern from(final RequestMapping requestMapping)
    {
        final String method = requestMapping.methods().isEmpty() ? "" : requestMapping.methods().iterator().next().name();
        final String pattern = requestMapping.patterns().isEmpty() ? "" : requestMapping.patterns().iterator().next();
        return new MethodAndPattern(method, pattern);
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Set<String> consumes()
    {
        return consumes;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Set<String> produces()
    {
        return produces;
    }

    @Override
    public String toString()
    {
        return "RequestMapping{" +
                "patterns=" + patterns +
                ", methods=" + methods +
                ", consumes=" + consumes +
                ", produces=" + produces +
                '}';
    }

    @Override
    public int compareTo(@NonNull final RequestMapping requestMapping)
    {
        return from(this).compareTo(from(requestMapping));
    }
}
