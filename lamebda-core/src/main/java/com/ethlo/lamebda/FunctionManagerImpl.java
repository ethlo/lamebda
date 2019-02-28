package com.ethlo.lamebda;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.functions.DirectoryResourceFunction;
import com.ethlo.lamebda.functions.ProjectStatusFunction;
import com.ethlo.lamebda.functions.SingleFileResourceFunction;
import com.ethlo.lamebda.functions.SingleResourceFunction;
import com.ethlo.lamebda.generator.GeneratorHelper;
import com.ethlo.lamebda.groovy.GroovyCompiler;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
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
    private final ProjectConfiguration projectConfiguration;
    private final ApplicationContext parentContext;
    private AnnotationConfigApplicationContext projectCtx;

    private Map<String, FunctionBundle> functions = new ConcurrentHashMap<>();
    private LamebdaResourceLoader lamebdaResourceLoader;
    private final FunctionMetricsService functionMetricsService = FunctionMetricsService.getInstance();

    private final GeneratorHelper generatorHelper;

    public FunctionManagerImpl(ApplicationContext parentContext, LamebdaResourceLoader lamebdaResourceLoader)
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
        this.parentContext = parentContext;

        initialize();
    }

    private void addBuiltinFunctions()
    {
        if (projectConfiguration.enableStaticResourceFunction())
        {
            addFunction(new FunctionBundle(ServerFunctionInfo.builtin("static-data", DirectoryResourceFunction.class), initWithProjectCfg(new DirectoryResourceFunction("/" + projectConfiguration.getStaticResourcesContext(), projectConfiguration.getStaticResourceDirectory()))));
        }

        if (projectConfiguration.enableInfoFunction())
        {
            // JSON data
            final String statusBasePath = "/status";
            addFunction(new FunctionBundle(ServerFunctionInfo.builtin("status-info", ProjectStatusFunction.class), initWithProjectCfg(new ProjectStatusFunction(statusBasePath + "/status.json", this, functionMetricsService))));

            // Page for viewing status
            addFunction(new FunctionBundle(ServerFunctionInfo.builtin("status-info-page", SingleResourceFunction.class), initWithProjectCfg(new SingleResourceFunction(statusBasePath + "/", HttpMimeType.HTML, IoUtil.classPathResource("/lamebda/templates/status.html").get()))));
        }
    }

    private void generateModels() throws IOException
    {
        if (generatorHelper != null)
        {
            runRegen(projectConfiguration, ".models.gen");
        }
    }

    private void generateHumanReadableApiDoc(final ProjectConfiguration projectConfiguration) throws IOException
    {
        if (generatorHelper != null)
        {
            runRegen(projectConfiguration, ".apidoc.gen");
        }
    }

    private void runRegen(final ProjectConfiguration projectConfiguration, final String generationCommandFile) throws IOException
    {
        final Optional<String> genFile = IoUtil.toString(projectConfiguration.getPath().resolve(generationCommandFile));
        final Optional<String> defaultGenFile = IoUtil.toString("/generation/" + generationCommandFile);
        final String[] args = genFile.map(s -> s.split(" ")).orElseGet(() -> defaultGenFile.get().split(" "));
        generatorHelper.generate(projectConfiguration.getPath(), args);
    }

    private <T extends ServerFunction & FunctionContextAware> T initWithProjectCfg(final T function)
    {
        function.init(projectConfiguration);
        if (function instanceof BaseServerFunction)
        {
            ((BaseServerFunction) function).initInternal(projectConfiguration);
        }
        return function;
    }

    private FunctionManagerImpl addFunction(FunctionBundle bundle)
    {
        final String name = bundle.getInfo().getName();
        final boolean exists = functions.put(name, bundle) != null;
        logger.info(exists ? "Handler {} was reloaded" : "{} was loaded", name);
        return this;
    }

    private void initialize()
    {
        final PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();

        final Path configFilePath = projectConfiguration.getPath().resolve(FileSystemLamebdaResourceLoader.DEFAULT_CONFIG_FILENAME);
        if (Files.exists(configFilePath))
        {
            propertyPlaceholderConfigurer.setLocation(new FileSystemResource(configFilePath));
        }

        this.projectCtx = new AnnotationConfigApplicationContext();
        this.projectCtx.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
        this.projectCtx.setParent(parentContext);
        this.projectCtx.setAllowBeanDefinitionOverriding(false);
        this.projectCtx.setClassLoader(lamebdaResourceLoader.getClassLoader());
        this.projectCtx.setId(projectConfiguration.getName());

        apiSpecProcessing();
        createProjectConfigBean();
        compileSources();

        // Scan for beans
        if (!projectConfiguration.getBasePackages().isEmpty())
        {
            logger.info("Scanning base packages: {}", StringUtils.collectionToCommaDelimitedString(projectConfiguration.getBasePackages()));
            this.projectCtx.scan(projectConfiguration.getBasePackages().toArray(new String[0]));
        }

        projectCtx.refresh();

        logger.info("Beans: {}", StringUtils.arrayToCommaDelimitedString(projectCtx.getBeanDefinitionNames()));
        logger.info("Controllers: {}", (projectCtx.getBeansWithAnnotation(Controller.class)));

        addBuiltinFunctions();

        // Load all functions
        projectCtx.getBeansOfType(ServerFunction.class).forEach((key, value) -> addFunction(new FunctionBundle(ServerFunctionInfo.ofClass((Class<ServerFunction>) value.getClass()), value)));
    }

    private void createProjectConfigBean()
    {
        final ConfigurableListableBeanFactory bf = projectCtx.getBeanFactory();
        bf.registerSingleton("projectConfiguration", lamebdaResourceLoader.getProjectConfiguration());
    }

    private void compileSources()
    {
        logger.info("Compiling sources");
        final GroovyClassLoader groovyClassLoader = (GroovyClassLoader) lamebdaResourceLoader.getClassLoader();
        GroovyCompiler.compile(groovyClassLoader, getProjectConfiguration().getGroovySourcePath(), getProjectConfiguration().getTargetClassDirectory());
    }

    private void apiSpecProcessing()
    {
        final Path apiPath = projectConfiguration.getPath().resolve(FileSystemLamebdaResourceLoader.SPECIFICATION_DIRECTORY).resolve(FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);
        final Path targetPath = projectConfiguration.getPath().resolve("target").resolve("api-doc");

        if (Files.exists(apiPath))
        {
            try
            {
                final Path marker = targetPath.resolve(".lastmodified");

                final OffsetDateTime specModified = lastModified(apiPath);
                final OffsetDateTime modelModified = lastModified(marker);

                if (specModified.isAfter(modelModified))
                {
                    generateModels();

                    generateHumanReadableApiDoc(projectConfiguration);
                }

                try
                {
                    Files.createFile(marker);
                    Files.setLastModifiedTime(marker, FileTime.from(specModified.toInstant()));
                }
                catch (FileAlreadyExistsException exc)
                {
                    // Ignore
                }
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException(exc);
            }

            registerSpecificationController(targetPath);
        }

        final String modelPath = projectConfiguration.getPath().resolve("target").resolve("generated-sources").resolve("models").toAbsolutePath().toString();
        lamebdaResourceLoader.addClasspath(modelPath);
        logger.info("Added model classpath {}", modelPath);
    }

    private void registerSpecificationController(final Path targetPath)
    {
        final String specificationBasePath = "/specification";

        if (Files.exists(targetPath))
        {
            addFunction(new FunctionBundle(ServerFunctionInfo.builtin("api-human-readable", DirectoryResourceFunction.class), initWithProjectCfg(new DirectoryResourceFunction(specificationBasePath + "/api/doc/", targetPath))));
        }

        final Optional<Path> specificationFile = lamebdaResourceLoader.getApiSpecification();
        specificationFile.ifPresent(f -> addFunction(new FunctionBundle(ServerFunctionInfo.builtin("api-yaml", SingleResourceFunction.class), initWithProjectCfg(new SingleFileResourceFunction(specificationBasePath + "/api/api.yaml", f)))));
    }

    private OffsetDateTime lastModified(final Path path) throws IOException
    {
        if (Files.exists(path))
        {
            return OffsetDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.UTC);
        }
        return OffsetDateTime.MIN;
    }

    @Override
    public ApplicationContext getProjectApplicationContext()
    {
        return projectCtx;
    }

    @Override
    public Optional<ServerFunction> getHandler(final String name)
    {
        final FunctionBundle res = functions.get(name);
        return res != null ? Optional.of(res.getFunction()) : Optional.empty();
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
            projectCtx.close();
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
