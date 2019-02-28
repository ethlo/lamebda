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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    private final String packageName = "mycontrollererer";

    @Before
    public void before() throws IOException
    {
        if (Files.exists(basepath))
        {
            IoUtil.deleteDirectory(basepath);
        }
        Files.createDirectories(basepath);

        move("SpringMvc.groovy");
        final ProjectConfiguration cfg = ProjectConfiguration.builder("lamebda", basepath).basePackages(packageName).build();
        functionManager = new FunctionManagerImpl(applicationContext, new FileSystemLamebdaResourceLoader(cfg));
    }

    @Test
    public void testInvokeSpringMvc() throws Exception
    {
        assertThat(functionManager.getHandler(packageName + ".SpringMvc")).isPresent();
        final MockHttpServletRequest req = new MockHttpServletRequest();
        final MockHttpServletResponse res = new MockHttpServletResponse();
        req.setRequestURI("/lamebda/lamebda-unit-test/test/123");
        req.setMethod("POST");
        req.setContentType("application/json");
        req.setContent("{\"payload\": \"hello world\"}".getBytes(StandardCharsets.UTF_8));
        functionManager.handle(new ServletHttpRequest("/gateway", req), new ServletHttpResponse(res));
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getContentAsString()).isEqualTo("{\"id\":\"123\"}");
    }

    private Path move(final String name) throws IOException
    {
        final Path dir = basepath.resolve(FileSystemLamebdaResourceLoader.SCRIPT_DIRECTORY).resolve(packageName);
        Files.createDirectories(dir);
        return Files.copy(Paths.get("src/test/groovy", packageName, name), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }
}
