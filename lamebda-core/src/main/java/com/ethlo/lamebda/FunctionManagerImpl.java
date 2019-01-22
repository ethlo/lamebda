package com.ethlo.lamebda;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.functions.BuiltInServerFunction;
import com.ethlo.lamebda.functions.DirectoryResourceFunction;
import com.ethlo.lamebda.functions.SingleFileResourceFunction;
import com.ethlo.lamebda.functions.SingleResourceFunction;
import com.ethlo.lamebda.functions.ProjectStatusFunction;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
import com.ethlo.lamebda.oas.ApiGenerator;
import com.ethlo.lamebda.oas.ModelGenerator;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
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

public class FunctionManagerImpl implements ConfigurableFunctionManager
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionManagerImpl.class);
    private final GroovyClassLoader groovyClassLoader;
    private final ProjectConfiguration projectConfiguration;

    private Map<Path, ServerFunction> functions = new ConcurrentHashMap<>();
    private LamebdaResourceLoader lamebdaResourceLoader;
    private final FunctionMetricsService functionMetricsService = FunctionMetricsService.getInstance();
    private final String statusBasePath = "/status";
    private final String specificationBasePath = "/specification";

    public FunctionManagerImpl(LamebdaResourceLoader lamebdaResourceLoader)
    {
        this.projectConfiguration = lamebdaResourceLoader.getProjectConfiguration();

        logger.info("Loading project: {}\n{}", projectConfiguration.getName(), projectConfiguration.toPrettyString());
        this.lamebdaResourceLoader = lamebdaResourceLoader;
        this.groovyClassLoader = new GroovyClassLoader();

        groovyClassLoader.addURL(lamebdaResourceLoader.getSharedClassPath());

        lamebdaResourceLoader.getLibUrls().forEach(url -> {
            logger.info("Adding lib classpath URL {}", url);
            groovyClassLoader.addURL(url);
        });

        lamebdaResourceLoader.setFunctionChangeListener(n -> {
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

                default:
                    throw new IllegalArgumentException("Unhandled event type: " + n.getChangeType());
            }
        });

        // Listen for specification changes
        lamebdaResourceLoader.setApiSpecificationChangeListener(n ->
        {
            logger.info("Specification file changed: {}", n.getPath());
            if (n.getChangeType() != ChangeType.DELETED)
            {
                try
                {
                    generateHumanReadableApiDoc(projectConfiguration, n.getPath());
                    generateModels(n.getPath());
                }
                catch (IOException exc)
                {
                    throw new UncheckedIOException(exc);
                }
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

        initialize();
    }

    private void addBuiltinFunctions() throws IOException
    {
        if (projectConfiguration.enableStaticResourceFunction())
        {
            addFunction(Paths.get("static-data"), withMinimalContext(new DirectoryResourceFunction("/" + projectConfiguration.getStaticResourcesContext(), projectConfiguration.getStaticResourceDirectory())));
        }

        if (projectConfiguration.enableInfoFunction())
        {
            final Path lamebdaTplDir = projectConfiguration.getPath().resolve("templates").resolve("lamebda");

            // Static page for viewing status
            createStaticOverrideableResource("welcome", "","welcome.html", lamebdaTplDir.resolve("welcome.html"));

            // JSON data
            addFunction(Paths.get("status-info"), withMinimalContext(new ProjectStatusFunction(statusBasePath + "/status.json", lamebdaResourceLoader, this, functionMetricsService)));

            // Static page for viewing status
            createStaticOverrideableResource("status", "/status","status.html", lamebdaTplDir.resolve("status.html"));
        }

        final Path apiPath = projectConfiguration.getPath().resolve(FileSystemLamebdaResourceLoader.SPECIFICATION_DIRECTORY).resolve(FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);
        if (Files.exists(apiPath))
        {
            generateModels(apiPath);
            generateHumanReadableApiDoc(projectConfiguration, apiPath);
        }
    }

    private void createStaticOverrideableResource(String name, String urlPath, String classPathResource, Path overrideFile)
    {
        if (Files.exists(overrideFile))
        {
            addFunction(Paths.get("custom-" + name), withMinimalContext(new SingleFileResourceFunction(urlPath, overrideFile)));
        }
        else
        {
            addFunction(Paths.get(name), withMinimalContext(new SingleResourceFunction(urlPath, HttpMimeType.HTML, IoUtil.classPathResource("/lamebda/templates/" + classPathResource))));
        }
    }

    private void load(final LamebdaResourceLoader lamebdaResourceLoader, final Path sourcePath)
    {
        final ServerFunction loaded = lamebdaResourceLoader.load(groovyClassLoader, sourcePath);
        addFunction(sourcePath, loaded);
    }

    private void generateModels(Path specificationFile) throws IOException
    {
        final URL classPathEntry = new ModelGenerator().generateModels(specificationFile);
        groovyClassLoader.addURL(classPathEntry);
        logger.info("Adding model classpath {}", classPathEntry);

    }

    private void generateHumanReadableApiDoc(final ProjectConfiguration projectConfiguration, Path specificationFile) throws IOException
    {
        final Path targetPath = Files.createTempDirectory("oas-tmp");

        final Path templatePath = projectConfiguration.getPath().resolve("templates").resolve("api-doc");
        final ApiGenerator gen = ApiGenerator.builder()
                .generatorName(projectConfiguration.getApiDocGenerator())
                .specPath(specificationFile)
                .targetPath(targetPath)
                .templatesPath(templatePath)
                .build();
        gen.generateApiDocumentation();

        addFunction(Paths.get("api-yaml"), withMinimalContext(new SingleResourceFunction(specificationBasePath + "/api/api.yaml", HttpMimeType.YAML, IoUtil.toByteArray(specificationFile))));

        addFunction(Paths.get("api-human-readable"), withMinimalContext(new DirectoryResourceFunction(specificationBasePath + "/api", targetPath)));
    }

    private <T extends ServerFunction & FunctionContextAware> T withMinimalContext(final T function)
    {
        function.setContext(new FunctionContext(projectConfiguration, new FunctionConfiguration()));
        return function;
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
        for (ServerFunctionInfo f : lamebdaResourceLoader.findAll(0, Integer.MAX_VALUE))
        {
            try
            {
                addFunction(f.getSourcePath(), lamebdaResourceLoader.load(groovyClassLoader, f.getSourcePath()));
            }
            catch (Exception exc)
            {
                logger.error("Error in function {}: {}", f.getSourcePath(), exc);
            }
        }

        try
        {
            addBuiltinFunctions();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
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

    @Override
    public void close()
    {
        try
        {
            this.groovyClassLoader.close();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        this.lamebdaResourceLoader.close();
    }

    @Override
    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
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
