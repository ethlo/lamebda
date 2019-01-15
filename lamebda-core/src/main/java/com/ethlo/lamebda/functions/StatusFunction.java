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

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ethlo.lamebda.FunctionManager;
import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
import com.ethlo.lamebda.reporting.FunctionStatusInfo;
import com.ethlo.lamebda.util.FileNameUtil;

public class StatusFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private final FunctionManager functionManager;
    private final LamebdaResourceLoader resourceLoader;
    private final String projectName;
    private final Path projectDir;

    public StatusFunction(final Path projectDir, String projectName, LamebdaResourceLoader resourceLoader, FunctionManager functionManager)
    {
        super("/" + projectName + "/info/");
        this.projectDir = projectDir;
        this.projectName = projectName;
        this.resourceLoader = resourceLoader;
        this.functionManager = functionManager;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response)
    {
        final int page = Integer.parseInt(request.param("page", "0"));
        final int size = Integer.parseInt(request.param("size", "25"));
        final FunctionManagerImpl fm = (FunctionManagerImpl) functionManager;
        final Map<Path, ServerFunction> functions = fm.getFunctions();
        final List<FunctionStatusInfo> functionList = getSingleHandlerInfo(page, size).stream().map(s ->
        {
            final boolean isLoaded = functions.get(s.getSourcePath()) != null;
            return new FunctionStatusInfo(projectDir, s).setRunning(isLoaded);
        }).collect(Collectors.toList());
        final Map<String, Object> res = new LinkedHashMap<>();
        res.put("project", Collections.singletonMap("name", projectName));
        res.put("functions", functionList);
        response.json(HttpStatus.OK, res);
    }

    private List<ServerFunctionInfo> getSingleHandlerInfo(int page, int pageSize)
    {
        return resourceLoader.findAll(page * pageSize, pageSize);
    }
}