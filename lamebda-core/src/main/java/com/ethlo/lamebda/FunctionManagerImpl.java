package com.ethlo.lamebda;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.functions.DirectoryResourceFunction;
import com.ethlo.lamebda.functions.ProjectStatusFunction;
import com.ethlo.lamebda.functions.SingleFileResourceFunction;
import com.ethlo.lamebda.functions.SingleResourceFunction;
import com.ethlo.lamebda.generator.GeneratorHelper;
import com.ethlo.lamebda.io.ChangeType;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.FileSystemNotificationAware;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.util.IoUtil;

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
    private final ProjectConfiguration projectConfiguration;

    private Map<String, FunctionBundle> functions = new ConcurrentHashMap<>();
    private LamebdaResourceLoader lamebdaResourceLoader;
    private final FunctionMetricsService functionMetricsService = FunctionMetricsService.getInstance();

    private final GeneratorHelper generatorHelper;

    public FunctionManagerImpl(LamebdaResourceLoader lamebdaResourceLoader)
    {
        this.projectConfiguration = lamebdaResourceLoader.getProjectConfiguration();
        final Path jarDir = projectConfiguration.getPath().resolve(".generator");
        if (Files.exists(jarDir))
        {
            this.generatorHelper = new GeneratorHelper(projectConfiguration.getJavaCmd(), jarDir);
        }
        else
        {
            logger.warn("No directory for code generation: {}", jarDir);
            generatorHelper = null;
        }

        this.lamebdaResourceLoader = lamebdaResourceLoader;

        registerChangeListenersIfApplicable(lamebdaResourceLoader);

        initialize();
    }

    private void registerChangeListenersIfApplicable(final LamebdaResourceLoader lamebdaResourceLoader)
    {
        if (lamebdaResourceLoader instanceof FileSystemNotificationAware)
        {
            final FileSystemNotificationAware fs = (FileSystemNotificationAware) lamebdaResourceLoader;
            fs.setFunctionChangeListener(n -> {
                switch (n.getChangeType())
                {
                    case CREATED:
                    case MODIFIED:
                        try
                        {
                            functionChanged(n.getPath());
                        }
                        catch (CompilationFailedException exc)
                        {
                            logger.warn("Unloading function {} due to script compilation error", n.getPath());
                            functionRemoved(n.getPath());
                            throw exc;
                        }
                        break;

                    case DELETED:
                        functionRemoved(n.getPath());
                        break;

                    default:
                        throw new IllegalArgumentException("Unhandled event type: " + n.getChangeType());
                }
            });

            // Listen for specification changes
            fs.setApiSpecificationChangeListener(n ->
            {
                logger.info("Specification file changed: {}", n.getPath());
                if (n.getChangeType() != ChangeType.DELETED)
                {
                    specificationChanged(n.getPath());
                }
            });
        }
    }

    private void addBuiltinFunctions()
    {
        if (projectConfiguration.enableStaticResourceFunction())
        {
            addFunction(new FunctionBundle(ScriptServerFunctionInfo.builtin("static-data", DirectoryResourceFunction.class), withMinimalContext(new DirectoryResourceFunction("/" + projectConfiguration.getStaticResourcesContext(), projectConfiguration.getStaticResourceDirectory()))));
        }

        if (projectConfiguration.enableInfoFunction())
        {
            final Path lamebdaTplDir = projectConfiguration.getPath().resolve("templates").resolve("lamebda");

            // JSON data
            final String statusBasePath = "/status";
            addFunction(new FunctionBundle(ScriptServerFunctionInfo.builtin("status-info", ProjectStatusFunction.class), withMinimalContext(new ProjectStatusFunction(statusBasePath + "/status.json", lamebdaResourceLoader, this, functionMetricsService))));

            // Page for viewing status
            addFunction(new FunctionBundle(ScriptServerFunctionInfo.builtin("status-info-page", SingleResourceFunction.class), withMinimalContext(new SingleResourceFunction(statusBasePath + "/", HttpMimeType.HTML, IoUtil.classPathResource("/lamebda/templates/status.html").get()))));
        }
    }

    @Override
    public void functionChanged(final Path sourcePath)
    {
        final ScriptServerFunctionInfo newInfo = ScriptServerFunctionInfo.ofScript(lamebdaResourceLoader, sourcePath);
        try
        {
            final ServerFunction function = lamebdaResourceLoader.load(sourcePath);
            addFunction(new FunctionBundle(newInfo, function));
        }
        catch (CompilationFailedException exc)
        {
            functionRemoved(sourcePath);
            throw exc;
        }
    }

    private void generateModels() throws IOException
    {
        if (generatorHelper != null)
        {
            runRegen(projectConfiguration, ".models.gen");
        }

        final String modelPath = projectConfiguration.getPath().resolve("target").resolve("generated-sources").resolve("models").toAbsolutePath().toString();
        lamebdaResourceLoader.addClasspath(modelPath);
        logger.info("Added model classpath {}", modelPath);
    }

    private void generateHumanReadableApiDoc(final ProjectConfiguration projectConfiguration, Path specificationFile) throws IOException
    {
        final String specificationBasePath = "/specification";

        addFunction(new FunctionBundle(ScriptServerFunctionInfo.builtin("api-yaml", SingleResourceFunction.class), withMinimalContext(new SingleFileResourceFunction(specificationBasePath + "/api/api.yaml", specificationFile))));

        if (generatorHelper != null)
        {
            runRegen(projectConfiguration, ".apidoc.gen");

            final Path targetPath = projectConfiguration.getPath().resolve("target").resolve("api-doc");
            addFunction(new FunctionBundle(ScriptServerFunctionInfo.builtin("api-human-readable", DirectoryResourceFunction.class), withMinimalContext(new DirectoryResourceFunction(specificationBasePath + "/api/doc/", targetPath))));
        }
    }

    private void runRegen(final ProjectConfiguration projectConfiguration, final String generationCommandFile) throws IOException
    {
        final Optional<String> genFile = IoUtil.toString(projectConfiguration.getPath().resolve(generationCommandFile));
        final Optional<String> defaultGenFile = IoUtil.toString("/generation/" + generationCommandFile);
        final String[] args = genFile.isPresent() ? genFile.get().split(" ") : defaultGenFile.get().split(" ");
        generatorHelper.generate(projectConfiguration.getPath(), args);
    }

    private <T extends ServerFunction & FunctionContextAware> T withMinimalContext(final T function)
    {
        function.setContext(new FunctionContext(projectConfiguration, new FunctionConfiguration()));
        return function;
    }

    private void reloadFunctions()
    {
        functions.forEach((name, func) -> {
            final FunctionBundle oldInfo = functions.get(name);
            if (oldInfo.getInfo() instanceof ScriptServerFunctionInfo)
            {
                final Path sourcePath = ((ScriptServerFunctionInfo) oldInfo.getInfo()).getSourcePath();
                final ScriptServerFunctionInfo newInfo = ScriptServerFunctionInfo.ofScript(lamebdaResourceLoader, sourcePath);
                final OffsetDateTime lastModified = ((ScriptServerFunctionInfo) oldInfo.getInfo()).getLastModified();
                if (lastModified.plusSeconds(1).isBefore(newInfo.getLastModified()))
                {
                    // The old is more than 1 second older than the new
                    functionChanged(sourcePath);
                }
            }
        });
    }

    public FunctionManagerImpl addFunction(FunctionBundle bundle)
    {
        final String name = bundle.getInfo().getName();
        final boolean exists = functions.put(name, bundle) != null;
        logger.info(exists ? "'{}' was reloaded" : "'{}' was loaded", name);
        return this;
    }

    private void initialize()
    {
        initialApiProcessing();

        for (AbstractServerFunctionInfo info : lamebdaResourceLoader.findAll(0, Integer.MAX_VALUE))
        {
            if (info instanceof ClassServerFunctionInfo)
            {
                addFunction(new FunctionBundle(info, lamebdaResourceLoader.prepare(info.getType())));
            }
            else
            {
                final ScriptServerFunctionInfo f = (ScriptServerFunctionInfo) info;
                try
                {
                    addFunction(new FunctionBundle(info, lamebdaResourceLoader.load(f.getSourcePath())));
                }
                catch (Exception exc)
                {
                    logger.error("Error in function {}: {}", f.getSourcePath(), exc);
                }
            }
        }

        addBuiltinFunctions();
    }

    private void initialApiProcessing()
    {
        final Path apiPath = projectConfiguration.getPath().resolve(FileSystemLamebdaResourceLoader.SPECIFICATION_DIRECTORY).resolve(FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);
        if (Files.exists(apiPath))
        {
            try
            {
                generateModels();
                generateHumanReadableApiDoc(projectConfiguration, apiPath);
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }
        }
    }

    @Override
    public void functionRemoved(final Path sourcePath)
    {
        final FunctionBundle func = functions.remove(sourcePath.toString());
        if (func != null)
        {
            logger.info("'{}' was unloaded", sourcePath);
        }
    }

    @Override
    public void specificationChanged(final Path path)
    {
        initialApiProcessing();

        logger.info("Reloading functions due to API specification change: {}", path);
        reloadFunctions();
    }

    @Override
    public Optional<ServerFunction> getFunction(final String name)
    {
        final FunctionBundle res = functions.get(name);
        return res != null ? Optional.of(res.getFunction()) : Optional.empty();
    }

    @Override
    public Optional<ServerFunction> getFunction(final Path sourcePath)
    {
        return getFunction(sourcePath.toString());
    }

    @Override
    public boolean handle(HttpRequest request, HttpResponse response) throws Exception
    {
        for (final FunctionBundle serverFunction : functions.values())
        {
            final boolean handled = doHandle(request, response, serverFunction.getFunction());
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
            this.lamebdaResourceLoader.close();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
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

    public Map<String, FunctionBundle> getFunctions()
    {
        return Collections.unmodifiableMap(functions);
    }
}
