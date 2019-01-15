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

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.util.IoUtil;

public class ApiGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(ApiGenerator.class);

    public void generateApiDocumentation(final Path specPath, Path targetFile) throws IOException
    {
        final Path targetBaseDir = Files.createTempDirectory("lamebda-oas-generator-tmp");
        final CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setInputSpec(specPath.toString());
        configurator.setGeneratorName("html");
        configurator.setOutputDir(targetBaseDir.toString());
        configurator.setModelPackage("spec");
        configurator.setValidateSpec(true);

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        final List<File> results = new DefaultGenerator().opts(clientOptInput).generate();
        logger.debug("Generated {} files", results.size());

        final Path apiDocPath = targetBaseDir;

        final Path targetDir = targetFile.getParent();
        Files.createDirectories(targetDir);
        Files.copy(apiDocPath.resolve("index.html"), targetFile, StandardCopyOption.REPLACE_EXISTING);
        IoUtil.deleteDirectory(targetBaseDir);
    }
}
