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

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.ethlo.lamebda.lifecycle.ProjectLoadedEvent;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCfg.class)
@AutoConfigureMockMvc
public class SpringMvcServerFunctionTest
{
    private final Path basepath = Paths.get("src/test/projects");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void before() throws IOException
    {
        final FunctionManagerDirector fmd = new FunctionManagerDirector(basepath, "lamebda", applicationContext);
        final FunctionManager fm = fmd.getFunctionManagers().values().iterator().next();
        new ProjectSetupService().onApplicationEvent(new ProjectLoadedEvent(fm.getProjectConfiguration(), fm.getProjectContext()));
    }

    @Test
    public void shouldCallController() throws Exception
    {
        this.mockMvc.
                perform(post("/lamebda/myproject/test/123").content("{\"payload\": 999}").contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("{\"id\":\"123\"}")));
    }
}
