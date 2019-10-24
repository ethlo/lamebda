package com.ethlo.lamebda.dao;

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalProjectDaoImpl implements LocalProjectDao
{
    private final Path rootDirectory;

    public LocalProjectDaoImpl(final Path rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public List<Path> getLocalProjectDirectories()
    {
        try (final Stream<Path> fs = Files.list(rootDirectory))
        {
            return fs
                    .filter(this::isValidProjectDir)
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isValidProjectDir(final Path p)
    {
        try
        {
            final boolean validDirectory = !Files.isHidden(p) && Files.isDirectory(p) && p.getParent().equals(rootDirectory);
            final boolean validCompressedFile = !Files.isHidden(p) && Files.isRegularFile(p) && p.getParent().equals(rootDirectory) && (p.toString().endsWith(".zip") || p.toString().endsWith(".jar"));
            return validDirectory || validCompressedFile;
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

}
