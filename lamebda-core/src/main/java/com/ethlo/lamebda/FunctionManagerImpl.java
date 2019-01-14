package com.ethlo.lamebda;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.functions.BuiltInServerFunction;
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

    private Map<Path, ServerFunction> functions = new ConcurrentHashMap<>();
    private LamebdaResourceLoader lamebdaResourceLoader;

    public FunctionManagerImpl(LamebdaResourceLoader lamebdaResourceLoader)
    {
        this.lamebdaResourceLoader = lamebdaResourceLoader;
        this.groovyClassLoader = new GroovyClassLoader();
        groovyClassLoader.addURL(lamebdaResourceLoader.getSharedClassPath());
        lamebdaResourceLoader.getLibUrls().forEach(url -> {
            logger.info("Adding classpath URL {}", url);
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
                            logger.info("Unloading function {} due to script compilation error", n.getPath());
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
    }

    private void load(final LamebdaResourceLoader lamebdaResourceLoader, final Path sourcePath)
    {
        // Load the function from source
        final ServerFunction loaded = lamebdaResourceLoader.load(groovyClassLoader, sourcePath);

        internalPostProcess(lamebdaResourceLoader, loaded, sourcePath);

        // Remove the last compilation error if any
        unload(sourcePath);

        // Add the function back
        addFunction(sourcePath, loaded);
    }

    private void processApiSpecification(Path specificationFile)
    {
        try
        {
            final URL classPathEntry = new ModelGenerator().generateModels(specificationFile);
            new ApiGenerator().generateApiDocumentation(specificationFile);

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


    private void internalPostProcess(final LamebdaResourceLoader lamebdaResourceLoader, final ServerFunction func, final Path sourcePath)
    {
        if (func instanceof FunctionContextAware)
        {
            ((FunctionContextAware) func).setContext(loadContext(lamebdaResourceLoader, func, sourcePath));
        }
    }

    private FunctionContext loadContext(final LamebdaResourceLoader lamebdaResourceLoader, final ServerFunction func, final Path sourcePath)
    {
        final FunctionConfiguration config = new FunctionConfiguration();

        final PropertyFile propertyFile = func.getClass().getAnnotation(PropertyFile.class);
        final Path basePath = sourcePath.getParent().getParent();
        final Path cfgFilePath = propertyFile != null ? basePath.resolve(propertyFile.value()) : basePath.resolve(FileSystemLamebdaResourceLoader.DEFAULT_CONFIG_FILENAME);
        String cfgContent;
        try
        {
            cfgContent = lamebdaResourceLoader.readSourceIfReadable(cfgFilePath);
        }
        catch (IOException exc)
        {
            throw new RuntimeException(exc);
        }

        if (cfgContent != null)
        {
            try
            {
                config.load(new StringReader(cfgContent));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unable to load property file " + cfgFilePath, e);
            }
        }
        config.put(FunctionContext.SCRIPT_SOURCE_PROPERTY_NAME, sourcePath);
        return new FunctionContext(config);
    }

    public FunctionManagerImpl addFunction(Path sourcePath, ServerFunction func)
    {
        final boolean exists = functions.put(sourcePath, func) != null;
        logger.info(exists ? "'{}' was reloaded" : "'{}' was loaded", sourcePath);
        return this;
    }

    @PostConstruct
    protected void loadAll()
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
    public void handle(HttpRequest request, HttpResponse response) throws Exception
    {
        for (final ServerFunction serverFunction : functions.values())
        {
            if (doHandle(request, response, serverFunction))
            {
                return;
            }
        }

        response.error(ErrorResponse.notFound("No function found to handle '" + request.path() + "'"));
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
