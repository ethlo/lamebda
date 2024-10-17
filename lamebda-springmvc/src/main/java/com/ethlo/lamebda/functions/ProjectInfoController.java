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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.ethlo.lamebda.LamebdaMetaAccessService;
import com.ethlo.lamebda.PebbleRenderer;
import com.ethlo.lamebda.Project;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ProjectManager;
import com.ethlo.lamebda.util.IoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "${lamebda.request-path}", produces = "application/json")
public class ProjectInfoController
{
    private static final Map<String, String> extensionMappings = new TreeMap<>()
    {{
        put("html", "text/html");
        put("css", "text/css");
        put("js", "application/javascript");
        put("gif", "image/gif");
        put("png", "image/png");
    }};
    private final ProjectManager projectManager;
    private final PebbleRenderer pebbleRenderer;
    private final LamebdaMetaAccessService lamebdaMetaAccessService;

    public ProjectInfoController(final ProjectManager projectManager, final LamebdaMetaAccessService lamebdaMetaAccessService)
    {
        this.projectManager = projectManager;
        this.pebbleRenderer = new PebbleRenderer(projectManager.getRootConfiguration().getUiBasePath(), true);
        this.lamebdaMetaAccessService = lamebdaMetaAccessService;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getHtml(final Locale locale, final HttpServletRequest request)
    {
        if (!lamebdaMetaAccessService.isIndexInfoAccessGranted(request))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(pebbleRenderer.render(getJson(request).getBody(), "index", locale));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJson(final HttpServletRequest request)
    {
        if (!lamebdaMetaAccessService.isIndexInfoAccessGranted(request))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final Map<String, Object> res = new LinkedHashMap<>();
        final Optional<String> optVersion = IoUtil.toString("lamebda-version.info");
        optVersion.ifPresent(versionStr -> res.put("lamebda_version", versionStr));
        res.put("startup_time", projectManager.getStartupTime());
        final String rootContext = getRootContext(request);
        res.put("lamebda_root_context", rootContext);
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

    private String getRootContext(final HttpServletRequest request)
    {
        return request.getContextPath() + "/" + projectManager.getRootConfiguration().getRequestPath();
    }

    private Map<String, Object> getProjectInfo(Project project)
    {
        final Map<String, Object> projectInfo = new LinkedHashMap<>();
        final ProjectConfiguration pc = project.getProjectConfiguration();
        projectInfo.put("alias", project.getAlias());
        projectInfo.put("name", pc.getProjectInfo().getName());
        projectInfo.put("base_packages", pc.getProjectInfo().getBasePackages());
        projectInfo.put("last_loaded", OffsetDateTime.ofInstant(Instant.ofEpochMilli(project.getProjectContext().getStartupDate()), ZoneId.systemDefault()));
        projectInfo.put("context_path", pc.getContextPath());
        projectInfo.put("version", pc.getProjectInfo().getVersion());
        projectInfo.put("has_openapi_spec", project.getApiSpecification().isPresent());
        projectInfo.put("request_mappings", project.getProjectContext().getBean("_all_mappings"));
        return projectInfo;
    }

    @GetMapping(value = "{project}/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getHealth(@PathVariable("project") final String projectAlias)
    {
        final Map<String, Object> res = new LinkedHashMap<>();
        res.put("alias", projectAlias);
        final List<String> all = projectManager.getProjectAliases();
        if (all.contains(projectAlias))
        {
            final Optional<Project> optProject = getProject(projectAlias);
            res.put("status", optProject.isPresent() ? "UP" : "DOWN");
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "{project}/api.yaml", produces = "text/yaml")
    public ResponseEntity<String> getSpecFile(@PathVariable("project") final String projectAlias, final HttpServletRequest request)
    {
        if (!lamebdaMetaAccessService.isProjectInfoAccessGranted(projectAlias, request))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final Optional<Project> optProject = getProject(projectAlias);

        return optProject.flatMap(project ->
                        project.getApiSpecification().map(r ->
                        {
                            try
                            {
                                final String content = preProcessApi(project, request.getContextPath(), IoUtil.toString(r.getInputStream(), StandardCharsets.UTF_8));
                                return ResponseEntity.ok(content);
                            }
                            catch (IOException e)
                            {
                                throw new UncheckedIOException(e);
                            }
                        }))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private String preProcessApi(final Project project, final String contextPath, String openApi)
    {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try
        {
            final JsonNode node = mapper.readTree(openApi);

            node.withArray("servers").forEach(server ->
            {
                final String url = server.path("url").textValue();
                final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
                final UriComponents uri = uriBuilder.build();
                final boolean isRelative = uri.getHost() == null;
                if (isRelative)
                {
                    // Prepend
                    final UriComponentsBuilder updated = uriBuilder
                            .replacePath("/")
                            .path(contextPath)
                            .path("/")
                            .path(project.getProjectConfiguration().getRootContextPath())
                            .path("/")
                            .path(project.getProjectConfiguration().getContextPath())
                            .path("/")
                            .path(uri.getPath() != null ? uri.getPath() : "");
                    ((ObjectNode) server).put("url", updated.build().toUri().toString());
                }
            });
            return mapper.writeValueAsString(node);
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
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
            final String html = pebbleRenderer.render(data, "swagger-ui", locale);
            return ResponseEntity.ok(html);
        }).orElse(ResponseEntity.notFound().build());
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