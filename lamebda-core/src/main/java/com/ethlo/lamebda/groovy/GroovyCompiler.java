package com.ethlo.lamebda.groovy;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.tools.GroovyClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class GroovyCompiler
{
    private static final Logger logger = LoggerFactory.getLogger(GroovyCompiler.class);

    public static List<Class<?>> compile(GroovyClassLoader cl, Path path)
    {
        final List<Class<?>> classes = new LinkedList<>();
        final CompilationUnit compileUnit = new CompilationUnit(cl);
        final List<Path> sourceFiles = findSourceFiles(path, FileSystemLamebdaResourceLoader.GROOVY_EXTENSION);
        for (Path sourceFile : sourceFiles)
        {
            logger.debug("Found source {}", sourceFile);
            compileUnit.addSource(sourceFile.toAbsolutePath().toString(), IoUtil.toString(sourceFile).orElseThrow(() -> new UncheckedIOException(new FileNotFoundException(sourceFile.toString()))));
        }

        final CompilerConfiguration ccfg = new CompilerConfiguration();
        try
        {
            ccfg.setTargetDirectory(Files.createTempDirectory("lamebda-compile-dir").toFile());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        compileUnit.setConfiguration(ccfg);
        compileUnit.compile();

        for (Object compileClass : compileUnit.getClasses())
        {
            final GroovyClass groovyClass = (GroovyClass) compileClass;
            logger.debug("Defining class {}", groovyClass.getName());

            try
            {
                cl.defineClass(groovyClass.getName(), groovyClass.getBytes());
            }
            catch (LinkageError exc)
            {
                logger.warn("Class already defined: {}", groovyClass.getName());
            }

            try
            {
                final Class<?> clazz = cl.loadClass(groovyClass.getName());
                classes.add(clazz);
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }

        return classes;
    }

    public static List<Path> findSourceFiles(Path sourceDir, String extension)
    {
        try (Stream<Path> stream = Files.walk(sourceDir))
        {
            return stream.filter(e -> e.getFileName().toString().endsWith(extension) && Files.isRegularFile(e)).collect(Collectors.toList());
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }
}
