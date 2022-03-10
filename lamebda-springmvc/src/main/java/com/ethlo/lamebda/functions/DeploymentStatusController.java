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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ethlo.lamebda.ProjectManager;

@RestController
@RequestMapping(value = "${lamebda.request-path}", produces = "application/json")
public class DeploymentStatusController
{
    private final ProjectManager projectManager;

    public DeploymentStatusController(final ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    @GetMapping("status")
    public Map<String, Object> get()
    {
        final Map<String, Object> res = new LinkedHashMap<>();
        res.put("deployments", projectManager.getProjects().values()
                .stream()
                .map(project -> Collections.singletonMap("name", project.getProjectConfiguration().getContextPath()))
                .collect(Collectors.toList()));
        return res;
    }
}