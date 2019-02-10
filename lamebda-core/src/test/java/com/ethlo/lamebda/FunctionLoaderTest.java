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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.junit.Test;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.util.IoUtil;

public class FunctionLoaderTest extends BaseTest
{
    public FunctionLoaderTest() throws IOException
    {
    }

    @Test
    public void testLoadOnCreate() throws Exception
    {

        deployConfig();
        deploySpec();
        ioWait();
        final Path sourcePath = deployFunc("Correct.groovy");
        ioWait();
        ioWait();
        final Map<Path, ServerFunction> functions = functionManager.getFunctions();
        assertThat(functions.keySet()).contains(sourcePath);

        final ServerFunction func = functions.get(sourcePath);
        final FunctionContext context = ((SimpleServerFunction) func).getContext();
        assertThat(context).isNotNull();
        final FunctionConfiguration cfg = context.getConfiguration();
        assertThat(cfg.getDateTime("start")).isNotNull();
        assertThat(cfg.getInt("min")).isNotNull();
        assertThat(cfg.getLong("max")).isNotNull();
        assertThat(cfg.getString("title")).isNotNull();
        assertThat(cfg.getProperty("title2")).isNull();
    }

    private void deployConfig() throws IOException
    {
        moveResource("config.properties", "");
    }

    private void deploySpec() throws IOException
    {
        moveResource("petstore-oas3.yaml", "specification", FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);
    }

    private void addShared() throws IOException
    {
        final Path libTargetDir = projectPath.resolve(FileSystemLamebdaResourceLoader.SHARED_DIRECTORY);
        IoUtil.copyFolder(Paths.get("src/test/groovy/shared"), libTargetDir);
    }

    @Test
    public void showCompilationError() throws Exception
    {
        deployFunc("Incorrect.groovy");
    }

    @Test
    public void testLoadOnModification() throws Exception
    {
        deployConfig();
        deploySpec();
        ioWait();
        final Path sourcePath = deployFunc("Correct.groovy");
        ioWait();

        final Map<Path, ServerFunction> functions = functionManager.getFunctions();
        final ServerFunction func = functions.get(sourcePath);
        final FunctionContext context = ((SimpleServerFunction) func).getContext();
        assertThat(context).isNotNull();
        assertThat(context.getConfiguration()).isNotNull();
        assertThat(context.getConfiguration().getProperty("foo")).isNull();
    }

    private void ioWait()
    {
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testUnloadOnRemoval() throws Exception
    {
        deployConfig();
        deploySpec();
        ioWait();
        final Path sourcePath = deployFunc("Correct.groovy");
        ioWait();
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).contains(sourcePath);

        // Remove it and assert unloaded
        remove(sourcePath);
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).doesNotContain(sourcePath);
    }

    @Test
    public void testUnloadOnError() throws Exception
    {
        deployConfig();
        deploySpec();
        ioWait();
        final Path sourcePath = deployFunc("Correct.groovy");
        ioWait();

        assertThat(functionManager.getFunctions().keySet()).contains(sourcePath);

        // Replace content with incorrect script
        Files.copy(Paths.get("src/test/groovy/Incorrect.groovy"), sourcePath, StandardCopyOption.REPLACE_EXISTING);
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).doesNotContain(sourcePath);
    }

    private Path deployFunc(final String name) throws IOException
    {
        addShared();
        final Path target = projectPath.resolve(FileSystemLamebdaResourceLoader.SCRIPT_DIRECTORY).resolve(name);
        Files.createDirectories(target.getParent());
        return Files.copy(Paths.get("src/test/groovy", name), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path moveResource(final String name, String folder) throws IOException
    {
        return moveResource(name, folder, name);
    }

    private Path moveResource(final String name, String folder, String filename) throws IOException
    {
        final Path target = projectPath.resolve(folder).resolve(filename);
        Files.createDirectories(target.getParent());
        ioWait();
        return Files.copy(Paths.get("src/test/resources", name), target, StandardCopyOption.REPLACE_EXISTING);
    }


    private void remove(Path sourcePath) throws IOException
    {
        if (Files.exists(sourcePath))
        {
            Files.delete(sourcePath);
        }
    }
}
