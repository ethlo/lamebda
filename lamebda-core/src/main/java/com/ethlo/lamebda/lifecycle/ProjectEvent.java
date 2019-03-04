package com.ethlo.lamebda.lifecycle;

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

import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.ethlo.lamebda.FunctionManager;
import com.ethlo.lamebda.ProjectConfiguration;

public abstract class ProjectEvent extends ApplicationEvent
{
    private final ProjectConfiguration projectConfiguration;
    private final AnnotationConfigApplicationContext projectCtx;
    private final FunctionManager functionManager;

    public ProjectEvent(ProjectConfiguration cfg, AnnotationConfigApplicationContext projectCtx, FunctionManager functionManager)
    {
        super(cfg.getPath());
        this.projectConfiguration = cfg;
        this.projectCtx = projectCtx;
        this.functionManager = functionManager;
    }

    public ProjectConfiguration getProjectConfiguration()
    {
        return projectConfiguration;
    }

    public AnnotationConfigApplicationContext getProjectCtx()
    {
        return projectCtx;
    }

    public FunctionManager getFunctionManager()
    {
        return functionManager;
    }
}
