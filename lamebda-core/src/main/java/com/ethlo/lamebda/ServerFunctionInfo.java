package com.ethlo.lamebda;

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

import java.util.Objects;

public class ServerFunctionInfo implements Comparable<ServerFunctionInfo>
{
    private final String name;
    private final Class<? extends ServerFunction> clazz;

    public ServerFunctionInfo(String name, final Class<? extends ServerFunction> clazz)
    {
        this.name = name;
        this.clazz = clazz;
    }

    public Class<? extends ServerFunction> getType()
    {
        return clazz;
    }

    public static ServerFunctionInfo builtin(String name, final Class<? extends ServerFunction> clazz)
    {
        return new ServerFunctionInfo(name, clazz);
    }

    public static ServerFunctionInfo ofClass(final Class<ServerFunction> function)
    {
        return new ServerFunctionInfo(function.getCanonicalName(), function);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ServerFunctionInfo that = (ServerFunctionInfo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(final ServerFunctionInfo serverFunctionInfo)
    {
        return name.compareTo(serverFunctionInfo.getName());
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "ServerFunctionInfo{name=" + name + "}";
    }
}
