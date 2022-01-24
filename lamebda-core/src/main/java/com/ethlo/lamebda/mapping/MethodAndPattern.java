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

import java.util.Comparator;
import java.util.Objects;

public class MethodAndPattern implements Comparable<MethodAndPattern>
{
    private final String method;
    private final String pattern;

    public MethodAndPattern(final String method, final String pattern)
    {
        this.method = method;
        this.pattern = pattern;
    }

    public String getMethod()
    {
        return method;
    }

    public String getPattern()
    {
        return pattern;
    }

    @Override
    public String toString()
    {
        return method + " " + pattern;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MethodAndPattern that = (MethodAndPattern) o;
        return Objects.equals(method, that.method) &&
                Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(method, pattern);
    }

    @Override
    public int compareTo(final MethodAndPattern b)
    {
        return Comparator.comparing(MethodAndPattern::getMethod).thenComparing(MethodAndPattern::getPattern).compare(this, b);
    }
}
