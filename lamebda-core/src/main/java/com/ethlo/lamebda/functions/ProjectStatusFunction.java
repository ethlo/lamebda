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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ethlo.lamebda.AbstractServerFunctionInfo;
import com.ethlo.lamebda.ConfigurableFunctionManager;
import com.ethlo.lamebda.FunctionManager;
import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.URLMappedServerFunction;
import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.loaders.LamebdaResourceLoader;
import com.ethlo.lamebda.reporting.FunctionMetricsService;
import com.ethlo.lamebda.reporting.FunctionStatusInfo;
import com.ethlo.lamebda.security.UsernamePasswordCredentials;

public class ProjectStatusFunction extends AdminSimpleServerFunction implements BuiltInServerFunction
{
    private final FunctionManager functionManager;
    private final LamebdaResourceLoader resourceLoader;
    private final ProjectConfiguration projectConfiguration;
    private final FunctionMetricsService functionMetricsService;

    public ProjectStatusFunction(String pattern, LamebdaResourceLoader resourceLoader, ConfigurableFunctionManager functionManager, FunctionMetricsService functionMetricsService)
    {
        super(pattern, functionManager.getProjectConfiguration().isInfoProtected());
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
        final List<FunctionStatusInfo> functionList = getFunctionInfoList(page, size).stream().map(s ->
        {
            final FunctionStatusInfo info = new FunctionStatusInfo(projectConfiguration.getPath(), s);

            final Optional<ServerFunction> funcOpt = ((FunctionManagerImpl) functionManager).getFunction(s.getName());
            final boolean isLoaded = funcOpt.isPresent();
            info.setRunning(isLoaded);

            if (funcOpt.isPresent())
            {
                final ServerFunction func = funcOpt.get();
                if (func instanceof URLMappedServerFunction)
                {
                    info.setRequestMappings(((URLMappedServerFunction) func).getUrlMapping());
                }
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

    private List<? extends AbstractServerFunctionInfo> getFunctionInfoList(int page, int pageSize)
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