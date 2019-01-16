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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.ethlo.lamebda.functions.StaticResourceFunction;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.test.MockHttpRequest;
import com.ethlo.lamebda.test.MockHttpResponse;
import com.ethlo.lamebda.util.IoUtil;

@SpringBootTest(classes = ServerFunctionTest.class)
@EnableAutoConfiguration
@RunWith(SpringRunner.class)
public class ServerFunctionTest
{
    private final Path basepath = Paths.get(System.getProperty("java.io.tmpdir"), "lamebda-unit-test");
    private FunctionManagerImpl functionManager;

    @Autowired
    private ApplicationContext applicationContext;

    public ServerFunctionTest() throws IOException
    {
        if (Files.exists(basepath))
        {
            IoUtil.deleteDirectory(basepath);
        }
        Files.createDirectories(basepath);

        functionManager = new FunctionManagerImpl(new FileSystemLamebdaResourceLoader((cl, s) -> s, f -> {
            applicationContext.getAutowireCapableBeanFactory().autowireBean(f);
            return f;
        }, basepath, "gateway"));

        functionManager.addFunction(Paths.get("static-resource-handler"), new StaticResourceFunction("", basepath.resolve("static")));
    }

    @Test
    public void testServingStaticResource() throws Exception
    {
        final MockHttpRequest req = new MockHttpRequest();
        final MockHttpResponse res = new MockHttpResponse();
        req.path("/static/incorrect.html");
        req.method("GET");
        functionManager.handle(req, res);
        assertThat(res.body()).contains("incorrect.html");
    }
}
