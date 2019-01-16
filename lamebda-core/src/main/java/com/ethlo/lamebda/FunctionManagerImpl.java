package com.ethlo.lamebda;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.functions.BuiltInServerFunction;
import com.ethlo.lamebda.functions.SingleResourceFunction;
import com.ethlo.lamebda.functions.StaticResourceFunction;
import com.ethlo.lamebda.functions.StatusFunction;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
import com.ethlo.lamebda.oas.ApiGenerator;
import com.ethlo.lamebda.oas.ModelGenerator;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

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

public class FunctionManagerImpl implements FunctionManager
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionManagerImpl.class);
    private final GroovyClassLoader groovyClassLoader;
    private final String projectName;

    private Map<Path, ServerFunction> functions = new ConcurrentHashMap<>();
    private LamebdaResourceLoader lamebdaResourceLoader;

    public FunctionManagerImpl(LamebdaResourceLoader lamebdaResourceLoader)
    {
        this.projectName = lamebdaResourceLoader.getRootPath().getFileName().toString();
        this.lamebdaResourceLoader = lamebdaResourceLoader;
        this.groovyClassLoader = new GroovyClassLoader();

        groovyClassLoader.addURL(lamebdaResourceLoader.getSharedClassPath());

        lamebdaResourceLoader.getLibUrls().forEach(url -> {
            logger.info("Adding lib classpath URL {}", url);
            groovyClassLoader.addURL(url);
        });

        if (lamebdaResourceLoader instanceof SourceChangeAware)
        {
            ((SourceChangeAware) lamebdaResourceLoader).setFunctionChangeListener(n -> {
                switch (n.getChangeType())
                {
                    case CREATED:
                    case MODIFIED:
                        try
                        {
                            load(lamebdaResourceLoader, n.getPath());
                        }
                        catch (CompilationFailedException exc)
                        {
                            logger.warn("Unloading function {} due to script compilation error", n.getPath());
                            unload(n.getPath());
                            throw exc;
                        }
                        break;

                    case DELETED:
                        unload(n.getPath());
                }
            });

            // Listen for specification changes
            lamebdaResourceLoader.setApiSpecificationChangeListener(n ->
            {
                logger.info("Specification file changed: {}", n.getPath());
                if (n.getChangeType() != ChangeType.DELETED)
                {
                    processApiSpecification(n.getPath());
                    reloadFunctions(n.getPath());
                }
            });

            // Listen for lib folder changes
            lamebdaResourceLoader.setLibChangeListener(n ->
            {
                if (n.getChangeType() == ChangeType.CREATED)
                {
                    groovyClassLoader.addURL(IoUtil.toURL(n.getPath()));
                }
            });
        }

        initialize();

        addBuiltinFunctions(lamebdaResourceLoader.getRootPath());
    }

    private void addBuiltinFunctions(Path projectDir)
    {
        addFunction(Paths.get("static-data"), new StaticResourceFunction(projectDir.getFileName().toString(), projectDir.resolve(FileSystemLamebdaResourceLoader.STATIC_DIRECTORY)));
        addFunction(Paths.get("status-info"), new StatusFunction(projectDir, projectName, lamebdaResourceLoader, this));
        addFunction(Paths.get("info-page"), new SingleResourceFunction("/" + projectName + "/", HttpMimeType.HTML, IoUtil.classPathResource("/lamebda/templates/info.html")));
    }

    private void load(final LamebdaResourceLoader lamebdaResourceLoader, final Path sourcePath)
    {
        final ServerFunction loaded = lamebdaResourceLoader.load(groovyClassLoader, sourcePath);
        addFunction(sourcePath, loaded);
    }

    private void processApiSpecification(Path specificationFile)
    {
        try
        {
            final URL classPathEntry = new ModelGenerator().generateModels(specificationFile);
            final Path targetFile = Files.createTempFile("oas-tmp", ".html");
            new ApiGenerator().generateApiDocumentation(specificationFile, targetFile);
            addFunction(Paths.get("api"), new SingleResourceFunction("/" + projectName + "/api/api.yaml", HttpMimeType.YAML, IoUtil.toByteArray(specificationFile)));
            addFunction(Paths.get("api-human-readable"), new SingleResourceFunction("/" + projectName + "/api/", HttpMimeType.HTML, IoUtil.toByteArray(targetFile)));
            Files.deleteIfExists(targetFile);
            groovyClassLoader.addURL(classPathEntry);
            logger.info("Adding model classpath {}", classPathEntry);
        }
        catch (IOException exc)
        {
            throw new RuntimeException("There was an error processing the API specification file " + specificationFile, exc);
        }
    }

    private void reloadFunctions(final Path path)
    {
        logger.info("Reloading functions due to API specification change: {}", path);
        functions.forEach((p, func) -> {
            if (!(func instanceof BuiltInServerFunction))
            {
                load(lamebdaResourceLoader, p);
            }
        });
    }

    public FunctionManagerImpl addFunction(Path sourcePath, ServerFunction func)
    {
        final boolean exists = functions.put(sourcePath, func) != null;
        logger.info(exists ? "'{}' was reloaded" : "'{}' was loaded", sourcePath);
        return this;
    }

    private void initialize()
    {
        final Optional<Path> apiSpecification = lamebdaResourceLoader.getApiSpecification();
        if (apiSpecification.isPresent())
        {
            processApiSpecification(apiSpecification.get());
        }

        for (ServerFunctionInfo f : lamebdaResourceLoader.findAll(0, Integer.MAX_VALUE))
        {
            try
            {
                addFunction(f.getSourcePath(), lamebdaResourceLoader.load(groovyClassLoader, f.getSourcePath()));
            }
            catch (Exception exc)
            {
                logger.error("Error in function {}: {}", f.getSourcePath(), exc.getMessage());
            }
        }
    }

    private void unload(final Path sourcePath)
    {
        final ServerFunction func = functions.remove(sourcePath);
        if (func != null)
        {
            logger.info("'{}' was unloaded", sourcePath);
        }
    }

    @Override
    public boolean handle(HttpRequest request, HttpResponse response) throws Exception
    {
        for (final ServerFunction serverFunction : functions.values())
        {
            final boolean handled = doHandle(request, response, serverFunction);
            if (handled)
            {
                return true;
            }
        }
        return false;
    }

    private boolean doHandle(HttpRequest request, HttpResponse response, ServerFunction f) throws Exception
    {
        try
        {
            final FunctionResult result = f.handle(request, response);
            if (result == null)
            {
                throw new IllegalStateException("A function should never return null. Expected FunctionResult");
            }
            return result == FunctionResult.PROCESSED;
        }
        catch (RuntimeException exc)
        {
            throw handleError(exc);
        }
    }

    private RuntimeException handleError(final RuntimeException exc)
    {
        Throwable cause = exc;
        if (exc instanceof UndeclaredThrowableException && exc.getCause() != null)
        {
            cause = exc.getCause();
        }

        if (cause instanceof RuntimeException)
        {
            throw (RuntimeException) cause;
        }
        throw new RuntimeException(cause);
    }

    public Map<Path, ServerFunction> getFunctions()
    {
        return Collections.unmodifiableMap(functions);
    }
}
