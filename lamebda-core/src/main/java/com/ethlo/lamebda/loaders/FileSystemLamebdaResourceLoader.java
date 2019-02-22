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
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.AbstractServerFunctionInfo;
import com.ethlo.lamebda.ClassServerFunctionInfo;
import com.ethlo.lamebda.FunctionContextAware;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.PropertyFile;
import com.ethlo.lamebda.ScriptServerFunctionInfo;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.functions.BuiltInServerFunction;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.FileNameUtil;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class FileSystemLamebdaResourceLoader implements LamebdaResourceLoader
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PROJECT_FILENAME = "project.properties";
    public static final String DEFAULT_CONFIG_FILENAME = "config.properties";

    public static final String JAR_EXTENSION = "jar";
    public static final String SCRIPT_EXTENSION = "groovy";
    public static final String PROPERTIES_EXTENSION = "properties";

    public static final String SCRIPT_DIRECTORY = "scripts";
    public static final String STATIC_DIRECTORY = "static";
    public static final String SPECIFICATION_DIRECTORY = "specification";
    public static final String SHARED_DIRECTORY = "shared";
    public static final String LIB_DIRECTORY = "lib";

    public static final String API_SPECIFICATION_JSON_FILENAME = "oas.json";
    public static final String API_SPECIFICATION_YAML_FILENAME = "oas.yaml";

    private final GroovyClassLoader groovyClassLoader;
    private final ProjectConfiguration projectConfiguration;
    private final FunctionPostProcessor functionPostProcessor;

    private Path scriptPath;
    private Path libPath;
    private List<ClassServerFunctionInfo> classFunctions;

    public FileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration, FunctionPostProcessor functionPostProcessor) throws IOException
    {
        this.projectConfiguration = projectConfiguration;
        this.functionPostProcessor = Assert.notNull(functionPostProcessor, "functionPostProcessor cannot be null");

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
        this.scriptPath = Files.createDirectories(projectPath.resolve(SCRIPT_DIRECTORY));
        final Path sharedPath = Files.createDirectories(projectPath.resolve(SHARED_DIRECTORY));
        this.libPath = Files.createDirectories(projectPath.resolve(LIB_DIRECTORY));

        final String scriptClassPath = scriptPath.toAbsolutePath().toString();
        this.groovyClassLoader.addClasspath(scriptClassPath);
        logger.info("Adding script classpath {}", scriptClassPath);

        final String sharedClassPath = sharedPath.toAbsolutePath().toString();
        groovyClassLoader.addClasspath(sharedClassPath);
        logger.info("Adding shared classpath {}", sharedClassPath);

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
                if (!Files.exists(target) || overwrite)
                {
                    logger.info("Unpacking {} to {}", ze.getName(), target);
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
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

    private ServerFunction instantiate(Class<? extends ServerFunction> clazz)
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

    private String toClassName(final Path sourcePath)
    {
        return scriptPath.relativize(sourcePath).toString().replace('/', '.').replace("." + SCRIPT_EXTENSION, "");
    }

    @Override
    public void close() throws IOException
    {
        this.groovyClassLoader.close();
    }

    private FunctionContext loadContext(final Class<?> functionClass)
    {
        final FunctionConfiguration functionConfiguration = loadFunctionConfig(functionClass);
        return new FunctionContext(projectConfiguration, functionConfiguration);
    }


    private FunctionConfiguration loadFunctionConfig(final Class<?> functionClass)
    {
        final FunctionConfiguration functionConfiguration = new FunctionConfiguration();

        final PropertyFile propertyFile = functionClass.getAnnotation(PropertyFile.class);
        final boolean required = propertyFile != null && propertyFile.required();
        final String filename = propertyFile != null ? propertyFile.value() : FileSystemLamebdaResourceLoader.DEFAULT_CONFIG_FILENAME;
        final Path cfgFilePath = getProjectConfiguration().getPath().resolve(filename);

        if (Files.exists(cfgFilePath))
        {
            try
            {
                final String cfgContent = readSource(cfgFilePath);
                functionConfiguration.load(new StringReader(cfgContent));
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }
        else if (required)
        {
            // Does not exist, but is required
            throw new UncheckedIOException(new FileNotFoundException(cfgFilePath.toString()));
        }

        return functionConfiguration;
    }

    @Override
    public List<? extends AbstractServerFunctionInfo> findAll(long offset, int size)
    {
        final List<AbstractServerFunctionInfo> all = new LinkedList<>();
        all.addAll(getServerFunctionScripts());
        all.addAll(getServerFunctionClasses());
        return all.stream().skip(offset).limit(size).collect(Collectors.toList());
    }

    private List<ScriptServerFunctionInfo> getServerFunctionScripts()
    {
        if (!Files.exists(scriptPath))
        {
            return Collections.emptyList();
        }

        try
        {
            return Files.list(scriptPath).filter(f -> FileNameUtil.getExtension(f.getFileName().toString()).equals(SCRIPT_EXTENSION))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(s -> ScriptServerFunctionInfo.ofScript(this, s))
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private List<ClassServerFunctionInfo> getServerFunctionClasses()
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
                        classFunctions.add(ClassServerFunctionInfo.ofClass((Class<ServerFunction>) Class.forName(classInfo.getName(), false, groovyClassLoader)));
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
    public ServerFunction load(Path sourcePath)
    {
        return prepare(loadClass(sourcePath));
    }

    @Override
    public ServerFunction prepare(Class<? extends ServerFunction> clazz)
    {
        final ServerFunction instance = instantiate(clazz);
        setContextIfApplicable(instance);
        return functionPostProcessor.process(instance);
    }

    private void setContextIfApplicable(final ServerFunction func)
    {
        if (func instanceof FunctionContextAware)
        {
            ((FunctionContextAware) func).setContext(loadContext(func.getClass()));
        }
    }

    @Override
    public String readSource(Path sourcePath) throws IOException
    {
        return new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
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
}
