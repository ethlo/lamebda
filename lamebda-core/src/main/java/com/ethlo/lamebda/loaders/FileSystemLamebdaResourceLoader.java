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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.functions.BuiltInServerFunction;
import com.ethlo.lamebda.io.FileSystemEvent;
import com.ethlo.lamebda.io.WatchDir;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class FileSystemLamebdaResourceLoader implements LamebdaResourceLoader
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PROJECT_FILENAME = "project.properties";
    public static final String DEFAULT_CONFIG_FILENAME = "application.properties";

    public static final String JAR_EXTENSION = "jar";
    public static final String GROOVY_EXTENSION = "groovy";
    public static final String JAVA_EXTENSION = "java";
    public static final String PROPERTIES_EXTENSION = "properties";

    public static final String SCRIPT_DIRECTORY = "scripts";
    public static final String STATIC_DIRECTORY = "static";
    public static final String SPECIFICATION_DIRECTORY = "specification";
    public static final String SHARED_DIRECTORY = "shared";
    public static final String LIB_DIRECTORY = "lib";

    public static final String API_SPECIFICATION_YAML_FILENAME = "oas.yaml";

    private GroovyClassLoader groovyClassLoader;
    private final ProjectConfiguration projectConfiguration;

    private Path libPath;
    private List<ServerFunctionInfo> classFunctions;
    private WatchDir watchDir;
    private Thread watchThread;

    public FileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration) throws IOException
    {
        this.projectConfiguration = projectConfiguration;

        final Path projectPath = projectConfiguration.getPath();
        if (!Files.exists(projectPath))
        {
            throw new FileNotFoundException("Cannot use " + projectPath.toAbsolutePath() + " as project directory as it does not exist");
        }

        logger.info("Loading project: {}", projectConfiguration.toPrettyString());

        this.groovyClassLoader = new GroovyClassLoader();

        final Path archivePath = projectPath.resolve(projectPath.getFileName() + "." + JAR_EXTENSION);
        if (Files.exists(archivePath))
        {
            decompress(projectPath, archivePath);
        }

        handleProject(projectPath);

        logger.info("Project directory: {}", projectPath);
    }

    private void decompress(final Path projectPath, final Path archivePath) throws IOException
    {
        unzipDirectory(archivePath, projectPath);
    }

    private void handleProject(final Path projectPath) throws IOException
    {
        Files.createDirectories(projectPath.resolve(SCRIPT_DIRECTORY));
        Files.createDirectories(projectPath.resolve(SHARED_DIRECTORY));
        this.libPath = Files.createDirectories(projectPath.resolve(LIB_DIRECTORY));

        getLibUrls().forEach(url ->
        {
            groovyClassLoader.addURL(url);
            logger.info("Adding library classpath {}", url);
        });
    }

    private void unzipDirectory(final Path archivePath, Path targetDir) throws IOException
    {
        final ZipFile zipFile = new ZipFile(archivePath.toString());
        final Enumeration zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements())
        {
            final ZipEntry ze = ((ZipEntry) zipEntries.nextElement());
            final Path target = targetDir.resolve(ze.getName());
            if (!ze.isDirectory())
            {
                Files.createDirectories(target.getParent());
                final InputStream in = zipFile.getInputStream(ze);
                final boolean overwrite = !target.getFileName().toString().endsWith("." + FileSystemLamebdaResourceLoader.PROPERTIES_EXTENSION);
                final boolean exists = Files.exists(target);
                if (!exists || overwrite)
                {
                    logger.debug("Unpacking {} to {}", ze.getName(), target);
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                else
                {
                    logger.info("Not overwriting {}", target);
                }
            }
        }
    }

    private List<URL> getLibUrls()
    {
        if (!Files.exists(libPath, LinkOption.NOFOLLOW_LINKS))
        {
            return Collections.emptyList();
        }
        return IoUtil.toClassPathList(libPath);
    }

    @Override
    public void close() throws IOException
    {
        this.watchThread = null;
        this.watchDir.close();
        this.groovyClassLoader.close();
    }

    @Override
    public List<ServerFunctionInfo> findAll(long offset, int size)
    {
        final List<ServerFunctionInfo> all = new LinkedList<>();
        all.addAll(getServerFunctionClasses());
        return all.stream().skip(offset).limit(size).collect(Collectors.toList());
    }

    private List<ServerFunctionInfo> getServerFunctionClasses()
    {
        if (this.classFunctions != null)
        {
            return this.classFunctions;
        }

        this.classFunctions = new LinkedList<>();
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().overrideClassLoaders(groovyClassLoader).scan())
        {
            scanResult.getClassesImplementing(ServerFunction.class.getCanonicalName()).forEach(classInfo ->
            {
                if (!classInfo.isAbstract() && !classInfo.implementsInterface(BuiltInServerFunction.class.getCanonicalName()))
                {
                    logger.info("Found function class: {}", classInfo.getName());

                    try
                    {
                        classFunctions.add(ServerFunctionInfo.ofClass((Class<ServerFunction>) Class.forName(classInfo.getName(), false, groovyClassLoader)));
                    }
                    catch (ClassNotFoundException e)
                    {
                        logger.error("Cannot load class {}", classInfo.getName());
                    }
                }
            });
        }

        return classFunctions;
    }

    @Override
    public Optional<Path> getApiSpecification()
    {
        final Path specPathYaml = projectConfiguration.getPath().resolve(SPECIFICATION_DIRECTORY).resolve(API_SPECIFICATION_YAML_FILENAME);
        if (Files.exists(specPathYaml))
        {
            return Optional.of(specPathYaml);
        }
        return Optional.empty();
    }

    @Override
    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
    }

    @Override
    public void addClasspath(final String path)
    {
        this.groovyClassLoader.addClasspath(path);
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return groovyClassLoader;
    }

    public FileSystemLamebdaResourceLoader setSourceChangeListener(final Consumer<FileSystemEvent> l) throws IOException
    {
        if (projectConfiguration.isListenForChanges())
        {
            listenForChanges(l, IoUtil.exists(projectConfiguration.getPath(), projectConfiguration.getScriptPath(), projectConfiguration.getSpecificationPath(), projectConfiguration.getLibraryPath()));
        }
        return this;
    }

    private void listenForChanges(final Consumer<FileSystemEvent> l, final Path[] exists) throws IOException
    {
        watchDir = new WatchDir(l, false, exists);
        this.watchThread = new Thread(() ->
        {
            logger.info("Watching {} for changes", StringUtils.arrayToCommaDelimitedString(exists));
            watchDir.processEvents();
        });
        this.watchThread.start();
    }
}
