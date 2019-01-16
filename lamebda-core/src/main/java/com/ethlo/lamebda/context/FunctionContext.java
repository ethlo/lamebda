package com.ethlo.lamebda.context;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
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
import java.nio.file.Paths;

public class FunctionContext
{
    private final FunctionConfiguration configuration;
    private final Path sourcePath;
    private final String projectName;
    private final String contextPath;

    public FunctionContext(final String contextPath, final String projectName, final Path sourcePath, FunctionConfiguration configuration)
    {
        this.contextPath = contextPath;
        this.configuration = configuration;
        this.sourcePath = sourcePath;
        this.projectName = projectName;
    }

    public FunctionConfiguration getConfiguration()
    {
        return configuration;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public Path getSourcePath()
    {
        return sourcePath;
    }

    public String getContextPath()
    {
        return this.contextPath;
    }
}
