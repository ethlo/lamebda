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

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.ethlo.lamebda.util.IoUtil;

@RunWith(SpringRunner.class)
public abstract class BaseTest
{
    private final Path rootPath = Paths.get("src/test/projects");
    private final Path projectPath = rootPath.resolve("myproject");
    protected FunctionManagerImpl functionManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApplicationContext parentContext;

    @Before
    public void setup()
    {
        try
        {
            deployGenerator();

            final ProjectConfiguration cfg = ProjectConfiguration.builder("lamebda", projectPath).listenForChanges(false).basePackages("acme").build();
            functionManager = new FunctionManagerImpl(parentContext, cfg);
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
    }

    private void deployGenerator() throws IOException
    {
        final Path targetDir = projectPath.resolve(".generator");
        final Path dlCachePath = targetDir.getParent().getParent().resolve("lamebda-dl-tmp");
        download("http://central.maven.org/maven2/org/openapitools/openapi-generator-cli/3.3.4/openapi-generator-cli-3.3.4.jar", dlCachePath);
        IoUtil.copyFolder(dlCachePath, targetDir);
    }

    private void download(String url, Path dir) throws IOException
    {
        Files.createDirectories(dir);
        final String filename = Paths.get(url).getFileName().toString();
        final Path target = dir.resolve(filename);
        if (!Files.exists(target))
        {
            logger.info("Downloading {} to {}", url, target);
            try (InputStream in = new URL(url).openStream())
            {
                Files.copy(in, target);
            }
        }
    }
}
