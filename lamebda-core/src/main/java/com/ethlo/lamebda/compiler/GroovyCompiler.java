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

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class GroovyCompiler implements LamebdaCompiler
{
    private static final Logger logger = LoggerFactory.getLogger(GroovyCompiler.class);
    private final GroovyClassLoader classLoader;
    private final Set<Path> sourcePaths;

    public GroovyCompiler(GroovyClassLoader classLoader, Set<Path> sourcePaths)
    {
        this.classLoader = classLoader;
        this.sourcePaths = sourcePaths;
        logger.debug("Groovy source paths: {}", StringUtils.collectionToCommaDelimitedString(this.sourcePaths));
    }

    @Override
    public void compile(Path classesDir)
    {
        final CompilationUnit compileUnit = new CompilationUnit(classLoader);
        final List<Path> sourceFiles = CompilerUtil.findSourceFiles(FunctionManagerImpl.GROOVY_EXTENSION, sourcePaths.toArray(new Path[0]));
        if (sourceFiles.isEmpty())
        {
            return;
        }
        logger.info("Found {} groovy files", sourceFiles.size());

        for (Path sourceFile : sourceFiles)
        {
            logger.debug("Found source {}", sourceFile);
            compileUnit.addSource(sourceFile.toAbsolutePath().toString(), IoUtil.toString(sourceFile).orElseThrow(() -> new UncheckedIOException(new FileNotFoundException(sourceFile.toString()))));
        }

        final CompilerConfiguration ccfg = new CompilerConfiguration();
        ccfg.setTargetDirectory(classesDir.toFile());
        compileUnit.setConfiguration(ccfg);
        compileUnit.compile();
        try
        {
            classLoader.addURL(classesDir.toAbsolutePath().toUri().toURL());
        }
        catch (MalformedURLException e)
        {
            throw new UncheckedIOException(e);
        }
    }

}
