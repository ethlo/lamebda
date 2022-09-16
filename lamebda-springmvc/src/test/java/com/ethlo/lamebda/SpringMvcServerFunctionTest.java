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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestCfg.class)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public class SpringMvcServerFunctionTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectManager projectManager;

    @Test
    public void lifecycles()
    {
        final Project project = projectManager.getByAlias("myproject").orElseThrow();
        assertFound(true);
        projectManager.unload(project);
        assertFound(false);
        projectManager.load(project);
        assertFound(true);
        projectManager.reload(project);
        assertFound(true);
        projectManager.load(project);
        assertFound(true);
    }

    private void assertFound(final boolean found)
    {
        try
        {
            mockMvc.
                    perform(post("/lamebda/myproject/baz/test/123")
                            .content("{\"payload\": 999}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(found ? status().isOk() : status().isNotFound());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldCallController() throws Exception
    {
        mockMvc.
                perform(post("/lamebda/myproject/baz/test/123")
                        .content("{\"payload\": 999}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("{\"id\":\"123\"}")));
    }

    @Test
    public void shouldCallController2() throws Exception
    {
        mockMvc.
                perform(post("/lamebda/myproject/baz/test/998877")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("{\"id\":\"998877\"}")));

    }

    @Test
    public void shouldCallOpenApiController() throws Exception
    {
        this.mockMvc.
                perform(get("/foo/bar/lamebda/myproject/api.yaml")
                        .contextPath("/foo/bar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("url: /foo/bar/lamebda/myproject/v1/mine")));
    }
}
