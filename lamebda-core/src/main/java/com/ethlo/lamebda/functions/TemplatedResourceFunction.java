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
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.TreeMap;

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.generator.RendererExec;
import com.ethlo.lamebda.util.IoUtil;

/**
 * Simple single templated resource function that will attempt to load the file first
 */
public class TemplatedResourceFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private final RendererExec renderer;
    private final ProjectConfiguration projectConfiguration;
    private final Path templateDirectory;
    private final String tplName;
    private final String contentType;
    private byte[] cache = null;
    private long lastModified = 0;

    public TemplatedResourceFunction(String urlPath, ProjectConfiguration projectConfiguration, String tplName, String contentType)
    {
        super(urlPath);
        this.projectConfiguration = projectConfiguration;
        this.tplName = tplName;
        this.contentType = contentType;
        this.templateDirectory = projectConfiguration.getPath().resolve("templates").resolve("lamebda");
        final Path rendererJar = projectConfiguration.getPath().getParent().resolve("lamebda-renderer.jar");
        if (Files.exists(rendererJar))
        {
            this.renderer = new RendererExec(projectConfiguration.getJavaCmd(), rendererJar, templateDirectory);
        }
        else
        {
            this.renderer = null;
        }
    }

    private byte[] render(String tplName, Map<String, Serializable> data) throws IOException
    {
        if (renderer == null)
        {
            return "No renderer available for template".getBytes(StandardCharsets.UTF_8);
        }
        final Path tplFile = templateDirectory.resolve(tplName);
        if (!Files.exists(tplFile))
        {
            return ("No " + tplName + " page found in " + templateDirectory.toString()).getBytes(StandardCharsets.UTF_8);
        }
        return doRender(tplName, data, tplFile);
    }

    private void extractTpl(String name) throws IOException
    {
        final Path builtInTplFile = templateDirectory.resolve("." + name);
        IoUtil.copyClasspathResource("/lamebda/templates/" + tplName, builtInTplFile);
    }

    private byte[] doRender(final String tplName, final Map<String, Serializable> data, final Path tplFile) throws IOException
    {
        final FileTime lastModified = Files.getLastModifiedTime(tplFile);
        if (cache == null || cache.length == 0 || lastModified.toMillis() > this.lastModified)
        {
            final Path tmpFile = Files.createTempFile("tpl_" + tplName, ".rendered");
            try
            {
                renderer.render(tplName, data, tmpFile);
                cache = IoUtil.toByteArray(tmpFile);
                this.lastModified = lastModified.toMillis();
            } finally
            {
                Files.deleteIfExists(tmpFile);
            }
        }
        return cache;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response)
    {
        final String parentContext = request.parentContext();
        final Map<String, Serializable> data = new TreeMap<>();
        data.put("projectConfig", projectConfiguration);
        data.put("baseUrl", Paths.get(parentContext, projectConfiguration.getRootContextPath(), projectConfiguration.getContextPath()).normalize().toString() + "/");
        response.setContentType(contentType);
        try
        {
            response.write(render(tplName, data));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}