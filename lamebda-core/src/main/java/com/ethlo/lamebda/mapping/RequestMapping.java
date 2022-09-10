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
import java.util.Objects;
import java.util.Set;

import com.ethlo.lamebda.HttpMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.lang.NonNull;

public class RequestMapping implements Comparable<RequestMapping>
{
    private final Set<String> patterns;
    private final Set<HttpMethod> methods;
    private final Set<String> consumes;
    private final Set<String> produces;

    public RequestMapping(final Set<String> patterns, final Set<HttpMethod> methods, final Set<String> consumes, final Set<String> produces)
    {
        this.patterns = patterns;
        this.methods = methods;
        this.consumes = consumes;
        this.produces = produces;
    }

    public static RequestMapping of(final HttpMethod method, String pattern)
    {
        return new RequestMapping(Collections.singleton(pattern), Collections.singleton(method), null, null);
    }

    public Set<String> getPatterns()
    {
        return patterns;
    }

    public Set<HttpMethod> getMethods()
    {
        return methods;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Set<String> getConsumes()
    {
        return consumes;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Set<String> getProduces()
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
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RequestMapping that = (RequestMapping) o;
        return Objects.equals(patterns, that.patterns) &&
                Objects.equals(methods, that.methods) &&
                Objects.equals(consumes, that.consumes) &&
                Objects.equals(produces, that.produces);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(patterns, methods, consumes, produces);
    }

    @Override
    public int compareTo(@NonNull final RequestMapping requestMapping)
    {
        return from(this).compareTo(from(requestMapping));
    }

    private MethodAndPattern from(final RequestMapping requestMapping)
    {
        final String method = requestMapping.getMethods().isEmpty() ? "" : requestMapping.getMethods().iterator().next().name();
        final String pattern = requestMapping.getPatterns().isEmpty() ? "" : requestMapping.getPatterns().iterator().next();
        return new MethodAndPattern(method, pattern);
    }
}
