package com.ethlo.lamebda;

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

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.util.IoUtil;

public class FunctionLoaderTest
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionLoaderTest.class);

    private final Path basepath = Paths.get(System.getProperty("java.io.tmpdir"), "lamebda-unit-test");
    private FunctionManagerImpl functionManager;

    public FunctionLoaderTest() throws IOException
    {
        if (Files.exists(basepath))
        {
            IoUtil.deleteDirectory(basepath);
        }
        Files.createDirectories(basepath);

        functionManager = new FunctionManagerImpl(new FileSystemLamebdaResourceLoader((cl, s) -> s
        , f -> f, basepath));
    }

    @Test
    public void testLoadOnCreate() throws Exception
    {
        logger.info("Adding properties and API specification file");
        moveResource("Correct.properties", "");
        moveResource("petstore-oas3.yaml", "specification", FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML);
        ioWait();

        deployFunc("Correct.groovy");
        ioWait();
        final Map<Path, ServerFunction> functions = functionManager.getFunctions();

        final Path sourcePath = getSourcePath("Correct.groovy");
        assertThat(functions.keySet()).containsExactly(sourcePath);

        final ServerFunction func = functions.get(sourcePath);
        final FunctionContext context = ((SimpleServerFunction) func).getContext();
        assertThat(context).isNotNull();
        final FunctionConfiguration cfg = context.getConfiguration();
        assertThat(cfg.getDateTime("start")).isNotNull();
        assertThat(cfg.getInt("min")).isNotNull();
        assertThat(cfg.getLong("max")).isNotNull();
        assertThat(cfg.getString("title")).isNotNull();
        assertThat(cfg.getString("title2")).isNull();
    }

    private void addLib() throws IOException
    {
        final Path libTargetDir = basepath.resolve("lib");
        final Path packageTargetDir = libTargetDir.resolve("mypackage");
        Files.createDirectories(packageTargetDir);
        Files.createDirectories(libTargetDir);
        Files.copy(Paths.get("src/test/groovy/lib","MyLib.groovy"), packageTargetDir.resolve("MyLib.groovy"), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void showCompilationError() throws Exception
    {
        deployFunc("Incorrect.groovy");
    }

    @Test
    public void testLoadOnModification() throws Exception
    {
        final String filename = "Correct.groovy";
        deployFunc(filename);
        ioWait();
        deployFunc(filename);
        ioWait();

        final Path sourcePath = getSourcePath(filename);
        final Map<Path, ServerFunction> functions = functionManager.getFunctions();
        final ServerFunction func = functions.get(sourcePath);
        final FunctionContext context = ((SimpleServerFunction) func).getContext();
        assertThat(context).isNotNull();
        assertThat(context.getConfiguration()).isNotNull();
        assertThat(context.getConfiguration().getDateTime("foo")).isNull();
    }

    private Path getSourcePath(final String filename)
    {
        return basepath.resolve(filename);
    }

    private void ioWait()
    {
        try
        {
            Thread.sleep(2_000);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testUnloadOnRemoval() throws Exception
    {
        final String filename = "Correct.groovy";
        final Path sourcePath = getSourcePath(filename);

        // Create file and wait for it to load
        deployFunc(filename);
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).contains(sourcePath);

        // Remove it and assert unloaded
        remove(filename);
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).doesNotContain(sourcePath);
    }

    @Test
    public void testUnloadOnError() throws Exception
    {
        final String filename = "Correct.groovy";
        final Path sourcePath = getSourcePath(filename);

        // Load correct script and verify loaded
        final Path target = deployFunc("Correct.groovy");
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).contains(sourcePath);

        // Replace content with incorrect script
        Files.copy(Paths.get("src/test/groovy/Incorrect.groovy"), target, StandardCopyOption.REPLACE_EXISTING);
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).doesNotContain(sourcePath);
    }

    private Path deployFunc(final String name) throws IOException
    {
        addLib();
        return Files.copy(Paths.get("src/test/groovy", name), basepath.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }

    private Path moveResource(final String name, String folder) throws IOException
    {
        return moveResource(name, folder, name);
    }

    private Path moveResource(final String name, String folder, String filename) throws IOException
    {
        final Path target = basepath.resolve(folder).resolve(filename);
        Files.createDirectories(target);
        ioWait();
        return Files.copy(Paths.get("src/test/resources", name), target, StandardCopyOption.REPLACE_EXISTING);
    }


    private void remove(final String name) throws IOException
    {
        final Path p = basepath.resolve(name);
        if (p.toFile().exists())
        {
            Files.delete(p);
        }
    }
}
