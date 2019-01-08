package com.ethlo.lamebda.oas;

/*-
 * #%L
 * lamebda-oas
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.compiler.ModelCompiler;
import com.ethlo.lamebda.util.IoUtil;
import groovy.lang.GroovyClassLoader;

public class ModelGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(ModelGenerator.class);

    public URL generateModels(final Path specPath) throws IOException
    {
        final Path targetBaseDir = Files.createTempDirectory("lamebda-oas-generator-tmp");

        final CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setInputSpec(specPath.toString());
        configurator.setGeneratorName("jaxrs-spec");
        configurator.setOutputDir(targetBaseDir.toString());
        configurator.setModelPackage("spec");
        configurator.setValidateSpec(true);
        configurator.addAdditionalProperty("useBeanValidation", true);
        configurator.addAdditionalProperty("performBeanValidation", true);
        configurator.addAdditionalProperty("useSwaggerAnnotations", false);

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        final List<File> results = new DefaultGenerator().opts(clientOptInput).generate();
        logger.debug("Generated {} classes", results.size());

        final Path modelsPath = targetBaseDir.resolve("src/gen/java/spec");
        final Path targetDir = specPath.getParent().resolve("target");
        new ModelCompiler(modelsPath.toFile(), targetDir.toFile()).compile();
        IoUtil.deleteDirectory(targetBaseDir);
        return targetDir.toUri().toURL();
    }
}
