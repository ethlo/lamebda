package com.ethlo.lamebda.compiler;

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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompilerUtil
{
    public static List<Path> findSourceFiles(String extension, Path... sourceDirectories)
    {
        final List<Path> result = new LinkedList<>();
        for(Path dir : sourceDirectories)
        {
            result.addAll(findSourceFiles(extension, dir));
        }
        return result;
    }

    public static List<Path> findSourceFiles(String extension, Path sourceDirectory)
    {
        if (Files.exists(sourceDirectory))
        {
            try (Stream<Path> stream = Files.walk(sourceDirectory))
            {
                return stream.filter(e -> e.getFileName().toString().endsWith(extension) && Files.isRegularFile(e)).collect(Collectors.toList());
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }
        return Collections.emptyList();
    }
}
