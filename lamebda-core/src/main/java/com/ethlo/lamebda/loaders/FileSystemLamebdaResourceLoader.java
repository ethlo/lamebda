package com.ethlo.lamebda.loaders;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 Morten Haraldsen (ethlo)
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
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ethlo.lamebda.FunctionContextAware;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.PropertyFile;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.util.FileNameUtil;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class FileSystemLamebdaResourceLoader extends AbstractFileSystemResourceLoader
{
    public FileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration) throws IOException
    {
        this(projectConfiguration, s -> s);
    }

    public FileSystemLamebdaResourceLoader(ProjectConfiguration projectConfiguration, FunctionPostProcessor functionPostProcessor) throws IOException
    {
        super(projectConfiguration, functionPostProcessor);
    }

    @Override
    public ServerFunction load(GroovyClassLoader classLoader, Path sourcePath)
    {
        final Class<ServerFunction> clazz = loadClass(classLoader, sourcePath);
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

    public FunctionContext loadContext(final Class<?> functionClass)
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

        return new FunctionContext(projectConfiguration, functionConfiguration);
    }

    @Override
    public String readSource(Path sourcePath) throws IOException
    {
        return new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
    }

    @Override
    public List<ServerFunctionInfo> findAll(long offset, int size)
    {
        try
        {
            return Files.list(scriptPath).filter(f -> FileNameUtil.getExtension(f.getFileName().toString()).equals(SCRIPT_EXTENSION))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .skip(offset)
                    .limit(size)
                    .map(ServerFunctionInfo::of)
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
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
    public URL getSharedClassPath()
    {
        try
        {
            return sharedPath.toUri().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public List<URL> getLibUrls()
    {
        if (!Files.exists(libPath, LinkOption.NOFOLLOW_LINKS))
        {
            return Collections.emptyList();
        }
        return IoUtil.toClassPathList(libPath);
    }

    @Override
    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
    }
    
    @Override
    public Path getScriptsPath()
    {
        return projectPath.resolve(SCRIPT_DIRECTORY);
    }
}