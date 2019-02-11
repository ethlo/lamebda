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
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import com.ethlo.time.ITU;

public class ServerFunctionInfo implements Comparable<ServerFunctionInfo>
{
    private final Path sourcePath;
    private OffsetDateTime lastModified;
    private boolean builtin;

    private ServerFunctionInfo(Path sourcePath, OffsetDateTime lastModified, boolean builtin)
    {
        this.sourcePath = sourcePath;
        this.lastModified = lastModified;
        this.builtin = builtin;
    }

    public static ServerFunctionInfo builtin(final String s)
    {
        return new ServerFunctionInfo(Paths.get(s), null, true);
    }

    public static ServerFunctionInfo of(final Path sourcePath)
    {
        if (Files.exists(sourcePath))
        {
            try
            {
                final FileTime modified = Files.getLastModifiedTime(sourcePath);
                return new ServerFunctionInfo(sourcePath, OffsetDateTime.ofInstant(modified.toInstant(), ZoneOffset.UTC), false);
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }
        return new ServerFunctionInfo(sourcePath, null, false);
    }

    public OffsetDateTime getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(OffsetDateTime lastModified)
    {
        this.lastModified = lastModified;
    }

    public Path getSourcePath()
    {
        return sourcePath;
    }

    public boolean isBuiltin()
    {
        return builtin;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ServerFunctionInfo that = (ServerFunctionInfo) o;
        return Objects.equals(sourcePath, that.sourcePath);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourcePath);
    }

    @Override
    public int compareTo(final ServerFunctionInfo serverFunctionInfo)
    {
        return sourcePath.compareTo(serverFunctionInfo.getSourcePath());
    }

    @Override
    public String toString()
    {
        return "ServerFunctionInfo{" +
                "sourcePath=" + sourcePath +
                ", lastModified=" + (lastModified != null ? ITU.formatUtcMilli(lastModified) : null) +
                ", builtin=" + builtin +
                '}';
    }
}
        