package com.ethlo.lamebda.compiler.java;

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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.compiler.CompilerUtil;
import com.ethlo.lamebda.compiler.LamebdaCompiler;

public class JavaCompiler implements LamebdaCompiler
{
    private static final Logger logger = LoggerFactory.getLogger(JavaCompiler.class);

    private final ClassLoader classLoader;
    private final Collection<Path> sourcePaths;

    public JavaCompiler(ClassLoader classLoader, Collection<Path> sourcePaths)
    {
        this.classLoader = classLoader;
        this.sourcePaths = sourcePaths;
        logger.debug("Java source paths: {}", StringUtils.collectionToCommaDelimitedString(sourcePaths));
    }

    @Override
    public void compile(Path classesDirectory)
    {
        final javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
        {
            throw new IllegalStateException("You need to run build with JDK or have tools.jar on the classpath");
        }

        try (final StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, null))
        {
            final JavaFileManager fileManager = new CustomClassloaderJavaFileManager(classLoader, standardFileManager);
            final List<File> sourceFiles = CompilerUtil.findSourceFiles(FunctionManagerImpl.JAVA_EXTENSION, sourcePaths.toArray(new Path[0])).stream().map(Path::toFile).collect(Collectors.toList());
            if (sourceFiles.isEmpty())
            {
                return;
            }
            logger.info("Found {} java files", sourceFiles.size());

            logger.debug("Compiling: {}", StringUtils.collectionToCommaDelimitedString(sourceFiles));

            final Iterable<? extends JavaFileObject> compilationUnits = standardFileManager.getJavaFileObjectsFromFiles(sourceFiles);

            List<String> compilerOptions = buildCompilerOptions(sourcePaths, classesDirectory);
            logger.debug("Compiler options: {}", StringUtils.collectionToCommaDelimitedString(compilerOptions));

            final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            final javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, compilerOptions, null, compilationUnits);
            final Boolean retVal = task.call();
            final StringBuilder s = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics())
            {
                s.append("\n").append(diagnostic);
            }

            if (!retVal)
            {
                throw new IllegalArgumentException("Processing failed: " + s.toString());
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> buildCompilerOptions(Collection<Path> sourcePath, Path classesDirectory)
    {
        final Map<String, String> compilerOpts = new LinkedHashMap<>();

        compilerOpts.put("d", classesDirectory.toAbsolutePath().toString());

        compilerOpts.put("sourcepath", StringUtils.collectionToDelimitedString(sourcePath, File.separator));

        final List<String> opts = new ArrayList<>(compilerOpts.size() * 2);
        for (Map.Entry<String, String> compilerOption : compilerOpts.entrySet())
        {
            opts.add("-" + compilerOption.getKey());
            String value = compilerOption.getValue();
            if (!StringUtils.isEmpty(value))
            {
                opts.add(value);
            }
        }
        return opts;
    }
}
