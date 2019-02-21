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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import com.ethlo.lamebda.loaders.LamebdaResourceLoader;

public abstract class AbstractServerFunctionInfo implements Comparable<AbstractServerFunctionInfo>
{
    private final String name;

    public abstract Class<?> getType();

    protected AbstractServerFunctionInfo(String name)
    {
        this.name = name;
    }

    public static ClassServerFunctionInfo builtin(String name, final Class<? extends ServerFunction> clazz)
    {
        return new ClassServerFunctionInfo(name, clazz);
    }

    public static ScriptServerFunctionInfo ofScript(LamebdaResourceLoader lamebdaResourceLoader, final Path sourcePath)
    {
        try
        {
            final FileTime modified = Files.getLastModifiedTime(sourcePath);
            return new ScriptServerFunctionInfo(lamebdaResourceLoader, sourcePath, OffsetDateTime.ofInstant(modified.toInstant(), ZoneOffset.UTC));
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    public static AbstractServerFunctionInfo ofClass(final Class<ServerFunction> function)
    {
        return new ClassServerFunctionInfo(function.getCanonicalName(), function);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractServerFunctionInfo that = (AbstractServerFunctionInfo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(final AbstractServerFunctionInfo serverFunctionInfo)
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