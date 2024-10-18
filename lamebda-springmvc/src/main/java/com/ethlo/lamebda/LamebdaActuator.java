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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.http.ResponseEntity;

import com.ethlo.lamebda.util.IoUtil;

@WebEndpoint(id = "lamebda")
public class LamebdaActuator
{
    private final ProjectManager projectManager;

    public LamebdaActuator(final ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    private static String getIso8601Duration(OffsetDateTime start, OffsetDateTime end)
    {
        final Period period = Period.between(start.toLocalDate(), end.toLocalDate());
        final Duration duration = Duration.between(start.toLocalTime(), end.toLocalTime());

        // Get the string representations of Period and Duration
        final String periodPart = period.toString();  // "PnYnMnD"
        String durationPart = duration.toString();  // "PTnHnMnS"

        // Remove the "P" from the Duration part to combine properly
        if (durationPart.startsWith("PT"))
        {
            durationPart = durationPart.substring(1); // remove the 'P' only
        }

        // If the duration contains valid time values, it must include "T"
        if (!durationPart.equals("T"))
        {
            return periodPart + durationPart;
        }
        else
        {
            return periodPart;  // only period part if duration is zero
        }
    }

    @ReadOperation
    public ResponseEntity<Map<String, Object>> getJson()
    {
        final Map<String, Object> res = new LinkedHashMap<>();
        final Optional<String> optVersion = IoUtil.toString("lamebda-version.info");
        optVersion.ifPresent(versionStr -> res.put("lamebda_version", versionStr));
        res.put("startup_time", projectManager.getStartupTime());
        res.put("uptime", getIso8601Duration(projectManager.getStartupTime(), OffsetDateTime.now()));
        res.put("projects", projectManager.getProjects().values()
                .stream()
                .map(this::getProjectInfo)
                .collect(Collectors.toList()));
        final List<String> down = projectManager.getProjectAliases();
        down.removeAll(projectManager.getProjects()
                .keySet()
                .stream().toList());
        res.put("projects_down", down);
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> getProjectInfo(Project project)
    {
        final Map<String, Object> projectInfo = new LinkedHashMap<>();
        final ProjectConfiguration projectConfiguration = project.getProjectConfiguration();
        projectInfo.put("alias", project.getAlias());
        projectInfo.put("name", projectConfiguration.getProjectInfo().getName());
        projectInfo.put("base_packages", projectConfiguration.getProjectInfo().getBasePackages());
        projectInfo.put("last_loaded", OffsetDateTime.ofInstant(Instant.ofEpochMilli(project.getProjectContext().getStartupDate()), ZoneId.systemDefault()));
        projectInfo.put("context_path", projectConfiguration.getContextPath());
        projectInfo.put("version", projectConfiguration.getProjectInfo().getVersion());
        projectInfo.put("request_mappings", project.getProjectContext().getBean("_all_mappings"));
        return projectInfo;
    }
}