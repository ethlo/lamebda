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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FilenameUtils;

public class IoUtil
{
    private IoUtil(){}
    
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

    public static String classPathResourceAsString(String path, Charset charset)
    {
        final InputStream in = IoUtil.class.getClassLoader().getResourceAsStream(path);
        if (in != null)
        {
            return toString(in, charset);
        }
        throw new UncheckedIOException(new FileNotFoundException(path));
    }

    public static void deleteDirectory(final Path directory) throws IOException
    {
        if (! Files.exists(directory) || !Files.isDirectory(directory))
        {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void copyFolder(Path src, Path dest) throws IOException {
        Files.createDirectories(dest);
        Files.walkFileTree(src, new SimpleFileVisitor<Path>()
        {
            @Override public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException
            {
                Files.createDirectories(path.normalize());
                return super.preVisitDirectory(path, basicFileAttributes);
            }

            @Override public FileVisitResult visitFile(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException
            {
                Path rel = src.relativize(path);
                final Path target = dest.resolve(rel);
                Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                return super.visitFile(path, basicFileAttributes);
            }
        });
    }

    public static void changeExtension(Path dir, final String sourceExtension, final String targetExtension) throws IOException
    {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
        {
            @Override public FileVisitResult visitFile(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException
            {
                final String filename = path.getFileName().toString();
                final String extension = FileNameUtil.getExtension(filename);
                if (extension.equals(sourceExtension))
                {
                    final String baseName = FileNameUtil.removeExtension(filename);
                    final Path target = path.getParent().resolve(baseName + FileNameUtil.EXTENSION_SEPARATOR + targetExtension);
                    Files.move(path, target, StandardCopyOption.ATOMIC_MOVE);
                }
                return super.visitFile(path, basicFileAttributes);
            }
        });
    }

    public static void copyClasspathResource(final String src, final Path target) throws IOException
    {
        final InputStream in = ClassLoader.getSystemResourceAsStream(src);
        if (in != null)
        {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        else
        {
            throw new IllegalArgumentException("Cannot find file " + src);
        }
    }

    public static Path ensureDirectoryExists(final Path path) throws IOException
    {
        Files.createDirectories(path);
        return path;
    }
}
