package com.ethlo.lamebda.loaders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.util.Assert;
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
    final Path scriptPath;
    final Path specificationPath;
    final Path sharedPath;
    final Path libPath;

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

        this.projectPath = projectPath;

        if (projectPath.toString().endsWith(".jar") || projectPath.toString().endsWith(".zip"))
        {
            final FileSystem filesystem = FileSystems.newFileSystem(projectPath, null);
            this.scriptPath = filesystem.getPath(SCRIPT_DIRECTORY);
            this.specificationPath = filesystem.getPath(SPECIFICATION_DIRECTORY);
            this.sharedPath = filesystem.getPath(SHARED_DIRECTORY);
            this.libPath = filesystem.getPath(LIB_DIRECTORY);
        }
        else
        {
            this.scriptPath = Files.createDirectories(projectPath.resolve(SCRIPT_DIRECTORY));
            this.specificationPath = Files.createDirectories(projectPath.resolve(SPECIFICATION_DIRECTORY));
            this.sharedPath = Files.createDirectories(projectPath.resolve(SHARED_DIRECTORY));
            this.libPath = Files.createDirectories(projectPath.resolve(LIB_DIRECTORY));
        }

        logger.info("Project directory: {}", projectPath);
    }

    protected ServerFunction instantiate(Class<ServerFunction> clazz)
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
    public Class<ServerFunction> loadClass(GroovyClassLoader classLoader, Path sourcePath)
    {
        try
        {
            final String source = readSource(sourcePath);
            final Class<?> clazz = classLoader.parseClass(source);
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

    }
}
