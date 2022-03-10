package com.ethlo.lamebda.functions;

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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.util.IoUtil;

@RestController
@RequestMapping(value = "/status", produces = "application/json")
public class ProjectStatusController
{
    private final ProjectConfiguration launchConfiguration;
    private final MultiValueMap<String, com.ethlo.lamebda.mapping.RequestMapping> mappings = new LinkedMultiValueMap<>();

    public ProjectStatusController(ProjectConfiguration launchConfiguration)
    {
        this.launchConfiguration = launchConfiguration;
    }

    @GetMapping("")
    public void getPage(HttpServletResponse response) throws IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        StreamUtils.copy(new ClassPathResource("/lamebda/templates/status.html").getInputStream(), response.getOutputStream());
    }

    @GetMapping("/api")
    public void getSwaggerUI(HttpServletResponse response) throws IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        StreamUtils.copy(new ClassPathResource("/lamebda/templates/swagger-ui.html").getInputStream(), response.getOutputStream());
    }

    @GetMapping("status.json")
    public Map<String, Object> get()
    {
        final Map<String, Object> res = new LinkedHashMap<>();
        final Map<String, Object> projectInfo = new LinkedHashMap<>();
        projectInfo.put("name", launchConfiguration.getProject().getName());
        projectInfo.put("configuration", launchConfiguration);
        projectInfo.put("version", launchConfiguration.getProject().getVersion());

        final Optional<String> optVersion = IoUtil.toString("lamebda-version.info");
        optVersion.ifPresent(versionStr ->
        {
            res.put("version", versionStr);
        });

        res.put("project", projectInfo);
        res.put("functions", mappings);
        return res;
    }

    public void add(final String beanName, final List<com.ethlo.lamebda.mapping.RequestMapping> mappings)
    {
        this.mappings.addAll(beanName, mappings);
    }
}