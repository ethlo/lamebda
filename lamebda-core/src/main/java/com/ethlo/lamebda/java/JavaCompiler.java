package com.ethlo.lamebda.java;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.groovy.GroovyCompiler;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;

public class JavaCompiler
{
    private static final Logger logger = LoggerFactory.getLogger(JavaCompiler.class);

    private static List<File> getCurrentClassPath()
    {
        final List<File> retVal = new ArrayList<>();
        final URLClassLoader cl = (URLClassLoader) JavaCompiler.class.getClassLoader();
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

    private static File[] getClassPathFiles()
    {
        final Set<File> files = new TreeSet<>(getCurrentClassPath());
        return files.toArray(new File[0]);
    }

    public static List<Class<?>> compile(ClassLoader cl, Path path)
    {
        final javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
        {
            throw new IllegalStateException("You need to run build with JDK or have tools.jar on the classpath");
        }

        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null))
        {
            final List<File> sourceFiles = GroovyCompiler.findSourceFiles(path, FileSystemLamebdaResourceLoader.JAVA_EXTENSION).stream().map(Path::toFile).collect(Collectors.toList());
            if (sourceFiles.isEmpty())
            {
                return Collections.emptyList();
            }

            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            final File[] classPathFiles = getClassPathFiles();

            final String compileClassPath = StringUtils.arrayToDelimitedString(classPathFiles, File.pathSeparator);
            logger.debug("Classpath: " + compileClassPath);

            List<String> compilerOptions = buildCompilerOptions(path, compileClassPath);

            final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
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

            return StreamSupport.stream(fileManager.list(StandardLocation.CLASS_OUTPUT, "",
                    Collections.singleton(JavaFileObject.Kind.CLASS), true
            ).spliterator(), false)
                    .map(JavaCompiler::loadClass).collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static Class<?> loadClass(final JavaFileObject javaFileObject)
    {
        try
        {
            return Class.forName(javaFileObject.getName());
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }


    private static List<String> buildCompilerOptions(Path sourcePath, String compileClassPath) throws IOException
    {
        final Map<String, String> compilerOpts = new LinkedHashMap<String, String>();
        compilerOpts.put("cp", compileClassPath);

        final String targetPath = Files.createTempDirectory("lamebda-java-compile-dir").toAbsolutePath().toString();
        compilerOpts.put("d", targetPath);

        compilerOpts.put("sourcepath", sourcePath.toAbsolutePath().toString());

        final List<String> opts = new ArrayList<String>(compilerOpts.size() * 2);
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
