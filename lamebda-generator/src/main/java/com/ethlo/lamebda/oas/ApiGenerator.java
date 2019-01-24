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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.util.IoUtil;

public class ApiGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(ApiGenerator.class);
    private Path templatesPath;
    private String generatorName;
    private Path specPath;
    private Path targetPath;

    private ApiGenerator(){};

    public void generateApiDocumentation() throws IOException
    {
        final Path tempDirectory = Files.createTempDirectory("lamebda-oas-generator-tmp");
        final CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setInputSpec(specPath.toString());
        configurator.setGeneratorName(generatorName);

        if (templatesPath != null)
        {
            configurator.setTemplateDir(templatesPath.toAbsolutePath().toString());
        }

        configurator.setOutputDir(tempDirectory.toString());
        configurator.setModelPackage("spec");
        configurator.setValidateSpec(true);

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        final List<File> results = new DefaultGenerator().opts(clientOptInput).generate();
        logger.debug("Generated {} files", results.size());

        Files.createDirectories(targetPath);
        IoUtil.copyFolder(tempDirectory, targetPath);
        IoUtil.deleteDirectory(tempDirectory);
    }

    public static ApiGeneratorBuilder builder()
    {
        return new ApiGeneratorBuilder();
    }

    public static final class ApiGeneratorBuilder
    {
        private Path templatesPath = null; // Use built-in by default
        private String generatorName = "html";
        private Path specPath;
        private Path targetPath;

        private ApiGeneratorBuilder()
        {
        }

        public ApiGeneratorBuilder templatesPath(Path templatesPath)
        {
            this.templatesPath = templatesPath;
            return this;
        }

        public ApiGeneratorBuilder generatorName(String generatorName)
        {
            this.generatorName = generatorName;
            return this;
        }

        public ApiGeneratorBuilder specPath(Path specPath)
        {
            this.specPath = specPath;
            return this;
        }

        public ApiGeneratorBuilder targetPath(Path targetPath)
        {
            this.targetPath = targetPath;
            return this;
        }

        public ApiGenerator build()
        {
            ApiGenerator apiGenerator = new ApiGenerator();
            apiGenerator.specPath = this.specPath;
            apiGenerator.targetPath = this.targetPath;
            apiGenerator.generatorName = this.generatorName;
            apiGenerator.templatesPath = this.templatesPath;
            return apiGenerator;
        }
    }
}
