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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import org.junit.Test;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.loaders.FileSystemClassResourceLoader;

public class FunctionLoaderTest
{
    private final File basepath = new File(System.getProperty("java.io.tmpdir"), "lamebda-unit-test");
    private FunctionManagerImpl functionManager;

    public FunctionLoaderTest() throws IOException
    {
        if (basepath.exists())
        {
            deleteDir(basepath.getCanonicalPath());
        }
        assertThat(basepath.mkdirs()).isTrue();

        functionManager = new FunctionManagerImpl(new FileSystemClassResourceLoader(f -> f, basepath.getAbsolutePath()));
    }

    @Test
    public void testLoadOnCreate() throws Exception
    {
        final String name = "Correct.properties";
        Files.copy(Paths.get("src/test/resources", name), Paths.get(basepath.getCanonicalPath(), name), StandardCopyOption.REPLACE_EXISTING);

        move("Correct.groovy");
        ioWait();
        final Map<String, ServerFunction> functions = functionManager.getFunctions();

        final String sourcePath = Paths.get(basepath.getAbsolutePath(), "Correct.groovy").toString();
        assertThat(functions.keySet()).containsExactly(sourcePath);

        final ServerFunction func = functions.get(sourcePath);
        final FunctionContext context = ((SimpleServerFunction)func).getContext();
        assertThat(context).isNotNull();
        final FunctionConfiguration cfg = context.getConfiguration();
        assertThat(cfg.getDateTime("start")).isNotNull();
        assertThat(cfg.getInt("min")).isNotNull();
        assertThat(cfg.getLong("max")).isNotNull();
        assertThat(cfg.getString("title")).isNotNull();
    }

    @Test
    public void showCompilationError() throws Exception
    {
        move("Incorrect.groovy");
    }

    @Test
    public void testLoadOnModification() throws Exception
    {
        move("Correct.groovy");
        ioWait();
        move("Correct.groovy");
        ioWait();
        return;
    }

    private void ioWait() throws InterruptedException
    {
        Thread.sleep(2_000);
    }

    @Test
    public void testUnloadOnRemoval() throws Exception
    {
        move("Correct.groovy");
        ioWait();
        remove("Correct.groovy");
        ioWait();
        final Map<String, ServerFunction> functions = functionManager.getFunctions();
        assertThat(functions.keySet()).doesNotContain("Correct");
    }

    @Test
    public void testUnloadOnError() throws Exception
    {
        final Path target = move("Correct.groovy");
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).contains(Paths.get(basepath.getAbsolutePath(), "Correct.groovy").toString());

        Files.copy(Paths.get("src/test/groovy/Incorrect.groovy"), target, StandardCopyOption.REPLACE_EXISTING);
        ioWait();
        assertThat(functionManager.getFunctions().keySet()).doesNotContain("foo.bar.Correct");
    }

    private Path move(final String name) throws IOException
    {
        return Files.copy(Paths.get("src/test/groovy", name), Paths.get(basepath.getCanonicalPath(), name), StandardCopyOption.REPLACE_EXISTING);
    }

    private void remove(final String name) throws IOException
    {
        final Path p = Paths.get(basepath.getCanonicalPath(), name);
        if (p.toFile().exists())
        {
            Files.delete(p);
        }
    }

    private void deleteDir(String dir) throws IOException
    {
        Path directory = Paths.get(dir);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
