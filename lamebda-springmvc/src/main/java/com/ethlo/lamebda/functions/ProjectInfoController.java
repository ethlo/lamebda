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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.ethlo.lamebda.PebbleRenderer;
import com.ethlo.lamebda.Project;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ProjectManager;
import com.ethlo.lamebda.util.IoUtil;

@RestController
@RequestMapping(value = "${lamebda.request-path}", produces = "application/json")
public class ProjectInfoController
{
    private final ResourceLoader resourceLoader;
    private final ProjectManager projectManager;
    private final PebbleRenderer pebbleRenderer = new PebbleRenderer(true);

    private static final Map<String, String> extensionMappings = new TreeMap<String, String>()
    {{
        put("html", "text/html");
        put("css", "text/css");
        put("js", "application/javascript");
        put("gif", "image/gif");
        put("png", "image/png");
    }};

    public ProjectInfoController(final ResourceLoader resourceLoader, final ProjectManager projectManager)
    {
        this.resourceLoader = resourceLoader;
        this.projectManager = projectManager;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getHtml(final Locale locale, final HttpServletRequest request)
    {
        final String template = getUiResource("index.html");
        return ResponseEntity.ok(pebbleRenderer.render(getJson(request), template, locale));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getJson(final HttpServletRequest request)
    {
        final Map<String, Object> res = new LinkedHashMap<>();
        final Optional<String> optVersion = IoUtil.toString("lamebda-version.info");
        optVersion.ifPresent(versionStr -> res.put("lamebda_version", versionStr));
        res.put("startup_time", projectManager.getStartupTime());
        final String rootContext = request.getContextPath() + "/" + projectManager.getRootConfiguration().getRequestPath();
        res.put("lamebda_root_context", rootContext);
        res.put("projects", projectManager.getProjects().values()
                .stream()
                .map(this::getProjectInfo)
                .collect(Collectors.toList()));
        return res;
    }

    private Map<String, Object> getProjectInfo(Project project)
    {
        final Map<String, Object> projectInfo = new LinkedHashMap<>();
        final ProjectConfiguration pc = project.getProjectConfiguration();
        projectInfo.put("name", pc.getProject().getName());
        projectInfo.put("last_loaded", OffsetDateTime.ofInstant(Instant.ofEpochMilli(project.getProjectContext().getStartupDate()), ZoneId.systemDefault()));
        projectInfo.put("context_path", pc.getContextPath());
        projectInfo.put("version", pc.getProject().getVersion());
        projectInfo.put("has_openapi_spec", getApiResource(project).exists());
        projectInfo.put("request_mappings", project.getProjectContext().getBean("_all_mappings"));
        return projectInfo;
    }

    @GetMapping(value = "{project}/api.yaml", produces = "text/yaml")
    public ResponseEntity<String> getSpecFile(@PathVariable("project") final String projectAlias)
    {
        final Optional<Project> optProject = getProject(projectAlias);

        return optProject.map(project ->
                {
                    final Resource resource = getApiResource(project);
                    if (resource.exists())
                    {
                        try
                        {
                            final String content = IoUtil.toString(resource.getInputStream(), StandardCharsets.UTF_8);
                            return ResponseEntity.ok(content);
                        }
                        catch (IOException e)
                        {
                            throw new UncheckedIOException(e);
                        }
                    }
                    return null;
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private Resource getApiResource(final Project project)
    {
        return project.getProjectContext().getResource("/specification/oas.yaml");
    }

    private Optional<Project> getProject(String projectAlias)
    {
        return projectManager.getProjects()
                .values()
                .stream()
                .filter(p -> p.getProjectConfiguration().getContextPath().equals(projectAlias))
                .findFirst();
    }

    @GetMapping(value = "{project}/swagger-ui", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getSwaggerUI(@PathVariable("project") final String projectAlias, Locale locale)
    {
        final Optional<Project> optProject = getProject(projectAlias);
        return optProject.map(project ->
        {
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("project", getProjectInfo(project));
            final String ui = getUiResource("swagger-ui.html");
            final String html = pebbleRenderer.render(data, ui, locale);
            return ResponseEntity.ok(html);
        }).orElse(ResponseEntity.notFound().build());
    }

    private String getUiResource(String path)
    {
        final Resource resource = resourceLoader.getResource(projectManager.getRootConfiguration().getUiBasePath() + "/" + path);
        try
        {
            return IoUtil.toString(resource.getInputStream(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

    }

    @GetMapping(value = "/swagger-ui-resources/**")
    public void serve(final HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        final AntPathMatcher apm = new AntPathMatcher();
        final String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

        final String classpath = projectManager.getRootConfiguration().getSwaggerUiPath() + "/" + finalPath;
        final ClassPathResource resource = new ClassPathResource(classpath, getClass().getClassLoader());

        if (resource.exists())
        {
            try (final InputStream in = resource.getInputStream(); final OutputStream out = response.getOutputStream())
            {
                final Optional<String> extOpt = IoUtil.getExtension(path);
                extOpt.ifPresent(ext -> response.setHeader("Content-Type", extensionMappings.get(ext)));
                IoUtil.copy(in, out);
            }
        }
        else
        {
            response.sendError(HttpStatus.NOT_FOUND.value());
        }
    }
}