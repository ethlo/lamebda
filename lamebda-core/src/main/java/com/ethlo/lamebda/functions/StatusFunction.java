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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ethlo.lamebda.ConfigurableFunctionManager;
import com.ethlo.lamebda.FunctionManager;
import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.reporting.FunctionStatusInfo;
import com.ethlo.lamebda.security.UsernamePasswordCredentials;

public class StatusFunction extends BasicAuthSimpleServerFunction implements BuiltInServerFunction
{
    private final FunctionManager functionManager;
    private final LamebdaResourceLoader resourceLoader;
    private final ProjectConfiguration projectConfiguration;
    private final FunctionMetricsService functionMetricsService;

    public StatusFunction(LamebdaResourceLoader resourceLoader, ConfigurableFunctionManager functionManager, FunctionMetricsService functionMetricsService)
    {
        super("/info/");
        this.resourceLoader = resourceLoader;
        this.functionManager = functionManager;
        this.projectConfiguration = functionManager.getProjectConfiguration();
        this.functionMetricsService = functionMetricsService;
        setContext(new FunctionContext(functionManager.getProjectConfiguration(), new FunctionConfiguration()));
    }

    @Override
    public void get(HttpRequest request, HttpResponse response)
    {
        final int page = Integer.parseInt(request.param("page", "0"));
        final int size = Integer.parseInt(request.param("size", "25"));
        final FunctionManagerImpl fm = (FunctionManagerImpl) functionManager;
        final Map<Path, ServerFunction> functions = fm.getFunctions();
        final List<FunctionStatusInfo> functionList = getFunctionInfoList(page, size).stream().map(s ->
        {
            final FunctionStatusInfo info = new FunctionStatusInfo(projectConfiguration.getPath(), s);

            final boolean isLoaded = functions.get(s.getSourcePath()) != null;
            info.setRunning(isLoaded);

            final ServerFunction func = ((FunctionManagerImpl) functionManager).getFunctions().get(s.getSourcePath());
            if (func instanceof URLMappedServerFunction)
            {
                info.setRequestMappings(((URLMappedServerFunction) func).getUrlMapping());
            }

            return info;
        }).collect(Collectors.toList());
        final Map<String, Object> res = new LinkedHashMap<>();
        final Map<String, Object> projectInfo = new LinkedHashMap<>();
        projectInfo.put("name", projectConfiguration.getName());
        projectInfo.put("configuration", projectConfiguration);
        res.put("project", projectInfo);
        res.put("functions", functionList);
        res.put("metrics", functionMetricsService.getMetrics());
        response.json(HttpStatus.OK, res);
    }

    private List<ServerFunctionInfo> getFunctionInfoList(int page, int pageSize)
    {
        return resourceLoader.findAll(page * pageSize, pageSize);
    }

    @Override
    protected boolean allow(String username, String password)
    {
        final UsernamePasswordCredentials adminCredentials = getContext().getProjectConfiguration().getAdminCredentials();
        return adminCredentials.matches(username, password);
    }
}