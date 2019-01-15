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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.servlet.ServletHttpRequest;
import com.ethlo.lamebda.servlet.ServletHttpResponse;
import com.ethlo.lamebda.spring.AutowireHelper;
import com.ethlo.lamebda.util.IoUtil;

@SpringBootTest(classes = SpringMvcServerFunctionTest.class)
@EnableAutoConfiguration
@RunWith(SpringRunner.class)
public class SpringMvcServerFunctionTest
{
    private final Path basepath = Paths.get(System.getProperty("java.io.tmpdir"), "lamebda-unit-test");
    private FunctionManagerImpl functionManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void before() throws IOException
    {
        if (Files.exists(basepath))
        {
            IoUtil.deleteDirectory(basepath);
        }
        Files.createDirectories(basepath);

        functionManager = new FunctionManagerImpl(new FileSystemLamebdaResourceLoader((cl, s) -> s, AutowireHelper.process(applicationContext), basepath));
    }

    private void ioWait() throws InterruptedException
    {
        Thread.sleep(2_000);
    }

    @Test
    public void testInvokeSpringMvc() throws Exception
    {
        final Path sourcePath = move("SpringMvc.groovy");
        ioWait();
        final Map<Path, ServerFunction> functions = functionManager.getFunctions();
        assertThat(functions.keySet()).contains(sourcePath);
        final MockHttpServletRequest req = new MockHttpServletRequest();
        final MockHttpServletResponse res = new MockHttpServletResponse();
        req.setRequestURI("/test/123");
        req.setMethod("POST");
        req.setContentType("application/json");
        req.setContent("{\"payload\": \"hello world\"}".getBytes(StandardCharsets.UTF_8));
        functionManager.handle(new ServletHttpRequest("/gateway", req), new ServletHttpResponse(res));
        assertThat(res.getContentAsString()).isEqualTo("{\"id\":\"123\"}");
    }

    private Path move(final String name) throws IOException
    {
        return Files.copy(Paths.get("src/test/groovy", name), basepath.resolve(FileSystemLamebdaResourceLoader.SCRIPT_DIRECTORY).resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }

    private void remove(final String name) throws IOException
    {
        final Path p = basepath.resolve(name);
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
