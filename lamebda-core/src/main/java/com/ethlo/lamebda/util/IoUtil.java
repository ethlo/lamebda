package com.ethlo.lamebda.util;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.FileSystemUtils;

import com.ethlo.lamebda.ProjectImpl;

public class IoUtil
{
    private IoUtil()
    {
    }

    public static byte[] toByteArray(final InputStream in)
    {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];

        try
        {
            while ((nRead = in.read(data, 0, data.length)) != -1)
            {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
        return buffer.toByteArray();
    }

    public static String toString(InputStream in, Charset charset)
    {
        return new String(toByteArray(in), charset);
    }

    public static URL toURL(final Path path)
    {
        try
        {
            return path.toUri().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static void deleteDirectory(final Path directory) throws IOException
    {
        if (!Files.exists(directory) || !Files.isDirectory(directory))
        {
            return;
        }
        FileSystemUtils.deleteRecursively(directory);
    }

    public static List<String> toClassPathList(final Path jarPath)
    {
        try (final Stream<Path> fs = Files.list(jarPath))
        {
            return fs.filter(p -> p.getFileName().toString().endsWith("." + ProjectImpl.JAR_EXTENSION))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static Optional<String> toString(final Path file)
    {
        try
        {
            return Optional.of(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            return Optional.empty();
        }
    }

    public static Optional<String> toString(final String path)
    {
        final String correctedPath = (!path.startsWith("/")) ? "/" + path : path;
        final InputStream in = IoUtil.class.getResourceAsStream(correctedPath);
        if (in != null)
        {
            final byte[] bytes = toByteArray(in);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        }
        return Optional.empty();
    }

    public static boolean isEmptyDir(final Path dir)
    {
        try (final Stream<Path> fs = Files.list(dir.getParent()))
        {
            return !fs.findFirst().isPresent();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static Optional<String> getExtension(final String path)
    {
        final Path p = Paths.get(path).getFileName();
        final String filename = p.getFileName().toString();
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex == -1)
        {
            return Optional.empty();
        }
        return Optional.of(filename.substring(lastIndex + 1));
    }

    public static void copy(final InputStream src, final OutputStream target)
    {
        try
        {
            byte[] buf = new byte[10_240];
            int len;
            while ((len = src.read(buf)) > 0)
            {
                target.write(buf, 0, len);
            }
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }
}
