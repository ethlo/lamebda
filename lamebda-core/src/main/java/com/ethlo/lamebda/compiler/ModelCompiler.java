package com.ethlo.lamebda.compiler;

/*-
 * #%L
 * lamebda-oas
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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelCompiler
{
    private static final Logger logger = LoggerFactory.getLogger(ModelCompiler.class);

    private final File sourceDir;
    private final File targetDir;

    public ModelCompiler(File sourceDir, File targetDir)
    {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
    }

    public void compile() throws IOException
    {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null))
        {
            final Set<File> sourceFiles = getSourceFiles();
            if (sourceFiles.isEmpty())
            {
                logger.info("No files to process");
                return;
            }

            logger.info("Found {} files", sourceFiles.size());
            logger.debug("Source files: {}", Arrays.toString(sourceFiles.toArray()));
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            final URL[] classPathFiles = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();

            final String compileClassPath = Arrays.stream(classPathFiles).map(url -> url.toString()).collect(Collectors.joining(File.pathSeparator));
            logger.debug("Classpath: {}", compileClassPath);

            List<String> compilerOptions = buildCompilerOptions(compileClassPath);

            final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            final JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, compilerOptions, null, compilationUnits);
            final Boolean retVal = task.call();
            final StringBuilder s = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics())
            {
                s.append("\n" + diagnostic);
            }

            if (!retVal)
            {
                throw new RuntimeException("Processing failed: " + s.toString());
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Set<File> getSourceFiles()
    {
        return new HashSet<>(Arrays.asList(sourceDir.listFiles(f -> f.getName().endsWith(".java"))));
    }

    private List<String> buildCompilerOptions(String compileClassPath)
    {
        final Map<String, String> compilerOpts = new LinkedHashMap<String, String>();
        compilerOpts.put("cp", compileClassPath);

        logger.info("Output directory: " + this.targetDir.getAbsolutePath());
        if (!targetDir.exists())
        {
            targetDir.mkdirs();
        }
        compilerOpts.put("d", targetDir.getAbsolutePath());

        try
        {
            compilerOpts.put("sourcepath", sourceDir.getCanonicalPath());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }

        final List<String> opts = new ArrayList<>(compilerOpts.size() * 2);
        for (Map.Entry<String, String> compilerOption : compilerOpts.entrySet())
        {
            opts.add("-" + compilerOption.getKey());
            String value = compilerOption.getValue();
            opts.add(value);
        }
        return opts;
    }

}
