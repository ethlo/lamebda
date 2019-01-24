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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.template.Renderer;

/**
 * Simple single templated resource function that will attempt to load the file first, then the classpath resource if not found
 */
public class TemplatedResourceFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private final Renderer renderer;
    private final ProjectConfiguration projectConfiguration;
    private final String tplName;
    private final String contentType;

    public TemplatedResourceFunction(String urlPath, ProjectConfiguration projectConfiguration, String tplName, String contentType)
    {
        super(urlPath);
        this.projectConfiguration = projectConfiguration;
        this.tplName = tplName;
        this.contentType = contentType;
        final Path tplPath = projectConfiguration.getPath().resolve("templates").resolve("lamebda");
        this.renderer = new Renderer(tplPath);

    }

    private byte[] render(String tplName, Map<String, Object> data)
    {
        return renderer.render(tplName, data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws IOException
    {
        final String parentContext = request.parentContext();
        final Map<String, Object> data = new TreeMap<>();
        data.put("projectConfig", projectConfiguration);
        data.put("baseUrl", Paths.get(parentContext, projectConfiguration.getRootContextPath(), projectConfiguration.getContextPath()).normalize().toString() + "/");
        response.setContentType(contentType);
        response.write(render(tplName, data));
    }
}
