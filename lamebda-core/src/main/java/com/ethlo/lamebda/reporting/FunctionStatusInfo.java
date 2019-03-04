package com.ethlo.lamebda.reporting;

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

import java.util.Set;

import com.ethlo.lamebda.BuiltInServerFunction;
import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.mapping.RequestMapping;

public class FunctionStatusInfo implements Comparable<FunctionStatusInfo>
{
    private final boolean builtin;
    private final String relPath;
    private final String name;
    private boolean running;
    private Set<RequestMapping> requestMappings;

    public FunctionStatusInfo(final ServerFunctionInfo info)
    {
        this.relPath = info.getType().getCanonicalName().replace('.', '/');
        this.name = info.getType().getSimpleName();
        this.builtin = BuiltInServerFunction.class.isAssignableFrom(info.getType());
    }

    public boolean isRunning()
    {
        return running;
    }

    public FunctionStatusInfo setRunning(final boolean running)
    {
        this.running = running;
        return this;
    }

    public String getName()
    {
        return name;
    }

    public String getRelPath()
    {
        return relPath;
    }

    public Set<RequestMapping> getRequestMappings()
    {
        return requestMappings;
    }

    public FunctionStatusInfo setRequestMappings(final Set<RequestMapping> requestMappings)
    {
        this.requestMappings = requestMappings;
        return this;
    }

    public boolean isBuiltin()
    {
        return builtin;
    }

    @Override
    public int compareTo(final FunctionStatusInfo b)
    {
        if (!isBuiltin() && b.isBuiltin())
        {
            return -1;
        }
        else if (isBuiltin() && !b.isBuiltin())
        {
            return 1;
        }
        return name.compareTo(b.getName());
    }
}

