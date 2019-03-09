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
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ethlo.lamebda.FunctionManagerImpl;

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

    public static void copyFolder(Path src, Path dest) throws IOException
    {
        Files.createDirectories(dest);
        Files.walkFileTree(src, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException
            {
                Files.createDirectories(path.normalize());
                return super.preVisitDirectory(path, basicFileAttributes);
            }

            @Override
            public FileVisitResult visitFile(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException
            {
                Path rel = src.relativize(path);
                final Path target = dest.resolve(rel);
                if (!Files.exists(target.getParent()))
                {
                    Files.createDirectories(target.getParent());
                }
                Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                return super.visitFile(path, basicFileAttributes);
            }
        });
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

    public static List<String> toClassPathList(final Path jarPath)
    {
        try
        {
            return Files
                    .list(jarPath)
                    .filter(p -> p.getFileName().toString().endsWith("." + FunctionManagerImpl.JAR_EXTENSION))
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

    public static Path[] exists(final Path... paths)
    {
        return Arrays.stream(paths).filter(Files::exists).toArray(Path[]::new);
    }
}
