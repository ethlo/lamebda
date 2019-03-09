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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.FunctionManagerImpl;

public class JavaCompiler implements LamebdaCompiler
{
    private static final Logger logger = LoggerFactory.getLogger(JavaCompiler.class);

    private final URLClassLoader classLoader;
    private final List<Path> sourcePaths;

    public JavaCompiler(URLClassLoader classLoader, List<Path> sourcePaths)
    {
        this.classLoader = classLoader;
        this.sourcePaths = sourcePaths;
        logger.debug("Java source paths: {}", StringUtils.collectionToCommaDelimitedString(sourcePaths));
    }

    private List<File> getCurrentClassPath(URLClassLoader cl)
    {
        final List<File> retVal = new ArrayList<>();
        try
        {
            for (URL url : cl.getURLs())
            {
                retVal.add(new File(url.toURI()));
            }
            return retVal;
        }
        catch (URISyntaxException exc)
        {
            throw new RuntimeException(exc.getMessage(), exc);
        }
    }

    private File[] getClassPathFiles()
    {
        final Set<File> files = new TreeSet<>(getCurrentClassPath((URLClassLoader) classLoader.getParent()));
        files.addAll(getCurrentClassPath(classLoader));
        return files.toArray(new File[0]);
    }

    @Override
    public void compile(Path classesDirectory)
    {
        final javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
        {
            throw new IllegalStateException("You need to run build with JDK or have tools.jar on the classpath");
        }

        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null))
        {
            final List<File> sourceFiles = CompilerUtil.findSourceFiles(FunctionManagerImpl.JAVA_EXTENSION, sourcePaths.toArray(new Path[0])).stream().map(Path::toFile).collect(Collectors.toList());
            if (sourceFiles.isEmpty())
            {
                return;
            }
            logger.info("Found {} java files", sourceFiles.size());

            logger.debug("Compiling: {}", StringUtils.collectionToCommaDelimitedString(sourceFiles));

            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            final File[] classPathFiles = getClassPathFiles();

            final String compileClassPath = StringUtils.arrayToDelimitedString(classPathFiles, File.pathSeparator);
            logger.debug("Classpath: " + compileClassPath);

            List<String> compilerOptions = buildCompilerOptions(sourcePaths, compileClassPath, classesDirectory);
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

    private static List<String> buildCompilerOptions(List<Path> sourcePath, String compileClassPath, Path classesDirectory)
    {
        final Map<String, String> compilerOpts = new LinkedHashMap<>();
        compilerOpts.put("cp", compileClassPath);

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
