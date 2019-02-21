package com.ethlo.lamebda.loaders;

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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public abstract class AbstractFileSystemResourceLoader implements LamebdaResourceLoader
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PROJECT_FILENAME = "project.properties";
    public static final String API_SPECIFICATION_YAML_FILENAME = "oas.yaml";
    static final String API_SPECIFICATION_JSON_FILENAME = "oas.json";
    static final String DEFAULT_CONFIG_FILENAME = "config.properties";

    static final String SCRIPT_EXTENSION = "groovy";
    public static final String JAR_EXTENSION = "jar";

    public static final String SCRIPT_DIRECTORY = "scripts";
    public static final String STATIC_DIRECTORY = "static";
    public static final String SPECIFICATION_DIRECTORY = "specification";
    public static final String SHARED_DIRECTORY = "shared";
    static final String LIB_DIRECTORY = "lib";

    final Path projectPath;
    private final Path workdir;
    Path scriptPath;
    Path specificationPath;
    Path sharedPath;
    Path libPath;

    protected final GroovyClassLoader groovyClassLoader;

    final ProjectConfiguration projectConfiguration;
    final FunctionPostProcessor functionPostProcessor;

    public AbstractFileSystemResourceLoader(ProjectConfiguration projectConfiguration, FunctionPostProcessor functionPostProcessor) throws IOException
    {
        this.projectConfiguration = projectConfiguration;
        this.functionPostProcessor = Assert.notNull(functionPostProcessor, "functionPostProcessor cannot be null");

        final Path projectPath = projectConfiguration.getPath();
        if (!Files.exists(projectPath))
        {
            throw new FileNotFoundException("Cannot use " + projectPath.toAbsolutePath() + " as project directory as it does not exist");
        }

        logger.info("Loading project: {}\n{}", projectConfiguration.getName(), projectConfiguration.toPrettyString());

        this.projectPath = projectPath;

        this.groovyClassLoader = new GroovyClassLoader();

        this.workdir = Files.createTempDirectory("lamebda-work-dir");

        if (projectPath.toString().endsWith(".jar") || projectPath.toString().endsWith(".zip"))
        {
            handleCompressedProject(projectPath);
        }
        else
        {
            handleExplodedProject(projectPath);
        }

        logger.info("Project directory: {}", projectPath);
    }

    private void handleCompressedProject(final Path projectPath) throws IOException
    {
        final FileSystem filesystem = FileSystems.newFileSystem(projectPath, null);
        this.scriptPath = filesystem.getPath(SCRIPT_DIRECTORY);
        this.specificationPath = filesystem.getPath(SPECIFICATION_DIRECTORY);
        this.sharedPath = filesystem.getPath(SHARED_DIRECTORY);
        this.libPath = filesystem.getPath(LIB_DIRECTORY);

        final List<URL> extractedLibs = unzip(projectPath, LIB_DIRECTORY);
        extractedLibs.forEach(groovyClassLoader::addURL);
    }

    private void handleExplodedProject(final Path projectPath) throws IOException
    {
        this.scriptPath = Files.createDirectories(projectPath.resolve(SCRIPT_DIRECTORY));
        this.specificationPath = Files.createDirectories(projectPath.resolve(SPECIFICATION_DIRECTORY));
        this.sharedPath = Files.createDirectories(projectPath.resolve(SHARED_DIRECTORY));
        this.libPath = Files.createDirectories(projectPath.resolve(LIB_DIRECTORY));

        final String scriptClassPath = scriptPath.toAbsolutePath().toString();
        this.groovyClassLoader.addClasspath(scriptClassPath);
        logger.info("Adding script classpath {}", scriptClassPath);

        final String sharedClassPath = sharedPath.toAbsolutePath().toString();
        groovyClassLoader.addClasspath(sharedClassPath);
        logger.info("Adding shared classpath {}", sharedClassPath);

        getLibUrls().forEach(url -> {
            groovyClassLoader.addURL(url);
            logger.info("Adding library classpath {}", url);
        });
    }

    private List<URL> unzip(final Path archivePath, String dir) throws IOException
    {
        final Path dirPath = workdir.resolve(dir);
        Files.createDirectories(dirPath);

        final ZipFile zipFile = new ZipFile(archivePath.toString());
        final Enumeration zipEntries = zipFile.entries();
        final List<URL> result = new ArrayList<>();
        while (zipEntries.hasMoreElements())
        {
            final ZipEntry ze = ((ZipEntry) zipEntries.nextElement());
            if (ze.getName().startsWith(dir) && !ze.isDirectory())
            {
                final InputStream in = zipFile.getInputStream(ze);

                final Path target = dirPath.resolve(Paths.get(ze.getName()).getFileName().toString());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                final URL url = target.toUri().toURL();
                logger.info("Unpacking {} to {}", ze.getName(), target);
                result.add(url);
            }
        }
        return result;
    }

    private List<URL> getLibUrls()
    {
        if (!Files.exists(libPath, LinkOption.NOFOLLOW_LINKS))
        {
            return Collections.emptyList();
        }
        return IoUtil.toClassPathList(libPath);
    }

    ServerFunction instantiate(Class<ServerFunction> clazz)
    {
        try
        {
            return ServerFunction.class.cast(clazz.newInstance());
        }
        catch (InstantiationException | IllegalAccessException exc)
        {
            throw new IllegalStateException("Cannot instantiate class " + clazz.getName(), exc);
        }
    }

    @Override
    public Class<ServerFunction> loadClass(Path sourcePath)
    {
        try
        {
            final String source = readSource(sourcePath);
            final Class<?> clazz = groovyClassLoader.parseClass(source);
            Assert.isTrue(ServerFunction.class.isAssignableFrom(clazz), "Class " + clazz.getName() + " must be instance of class ServerFunction");

            final String actualClassName = clazz.getCanonicalName();

            final String expectedClassName = toClassName(sourcePath);
            Assert.isTrue(actualClassName.equals(expectedClassName), "Unexpected class name '" + actualClassName + "' in file " + sourcePath + ". Expected " + expectedClassName);

            return (Class<ServerFunction>) clazz;
        }
        catch (IOException exc)
        {
            throw new IllegalStateException("Cannot load class " + sourcePath, exc);
        }
    }

    protected String toClassName(final Path sourcePath)
    {
        return scriptPath.relativize(sourcePath).toString().replace('/', '.').replace("." + SCRIPT_EXTENSION, "");
    }

    @Override
    public void close() throws IOException
    {
        this.groovyClassLoader.close();
    }
}
