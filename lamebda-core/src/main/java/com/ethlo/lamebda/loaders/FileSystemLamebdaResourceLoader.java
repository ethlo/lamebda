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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class FileSystemLamebdaResourceLoader implements LamebdaResourceLoader
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PROJECT_FILENAME = "project.properties";
    public static final String DEFAULT_CONFIG_FILENAME = "application.properties";

    public static final String JAR_EXTENSION = "jar";
    public static final String GROOVY_EXTENSION = "groovy";
    public static final String JAVA_EXTENSION = "java";
    public static final String PROPERTIES_EXTENSION = "properties";

    public static final String STATIC_DIRECTORY = "static";
    public static final String SPECIFICATION_DIRECTORY = "specification";
    public static final String LIB_DIRECTORY = "lib";

    public static final String API_SPECIFICATION_YAML_FILENAME = "oas.yaml";

    private GroovyClassLoader groovyClassLoader;
    private final ProjectConfiguration projectConfiguration;

    private Path libPath;

    public FileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration) throws IOException
    {
        this.projectConfiguration = projectConfiguration;

        final Path projectPath = projectConfiguration.getPath();
        if (!Files.exists(projectPath))
        {
            throw new FileNotFoundException("Cannot use " + projectPath.toAbsolutePath() + " as project directory as it does not exist");
        }

        logger.debug("Loading project: {}", projectConfiguration.toPrettyString());

        this.groovyClassLoader = new GroovyClassLoader();

        final Path archivePath = projectPath.resolve(projectPath.getFileName() + "." + JAR_EXTENSION);
        if (Files.exists(archivePath))
        {
            decompress(projectPath, archivePath);
        }

        handleProject(projectPath);

        logger.debug("Project directory: {}", projectPath);
    }

    private void decompress(final Path projectPath, final Path archivePath) throws IOException
    {
        unzipDirectory(archivePath, projectPath);
    }

    private void handleProject(final Path projectPath)
    {
        this.libPath = projectPath.resolve(LIB_DIRECTORY);
        if (Files.isDirectory(libPath))
        {
            getLibUrls().forEach(url ->
            {
                groovyClassLoader.addClasspath(url);
                logger.info("Adding library classpath {}", url);
            });
        }
        else
        {
            logger.info("Lib directory {} does not exist. Skipping", libPath);
        }
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

    private List<String> getLibUrls()
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
        logger.info("Closing class loader");
        this.groovyClassLoader.close();
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
}
