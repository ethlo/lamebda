package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-core
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.util.IoUtil;

public abstract class BaseTest
{
    private final Path rootPath = Paths.get(System.getProperty("java.io.tmpdir"));
    protected final Path projectPath = rootPath.resolve("lamebda-unit-test");
    protected final FunctionManagerImpl functionManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public BaseTest() throws IOException
    {
        deployGenerator();

        try
        {
            if (Files.exists(projectPath))
            {
                IoUtil.deleteDirectory(projectPath);
            }
            Files.createDirectories(projectPath);

            final ProjectConfiguration cfg = ProjectConfiguration.builder("lamebda", projectPath).build();
            functionManager = new FunctionManagerImpl(new FileSystemLamebdaResourceLoader(cfg, (cl, s) -> s, f -> f));
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    private void deployGenerator() throws IOException
    {
        final Path target = rootPath.resolve("openapi-generator-cli.jar");
        if (!Files.exists(target))
        {
            final String url = "http://central.maven.org/maven2/org/openapitools/openapi-generator-cli/3.3.4/openapi-generator-cli-3.3.4.jar";
            logger.info("Downloading {}", url);
            try (InputStream in = new URL(url).openStream())
            {
                Files.copy(in, target);
            }
        }
    }
}
