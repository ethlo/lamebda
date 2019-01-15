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

import java.nio.file.Path;
import java.time.OffsetDateTime;

import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.util.FileNameUtil;

public class FunctionStatusInfo
{
    private OffsetDateTime lastModified;
    private boolean running;
    private String relPath;
    private String name;

    public FunctionStatusInfo(Path projectDir, final ServerFunctionInfo info)
    {
        name = FileNameUtil.removeExtension(info.getSourcePath().getFileName().toString());
        relPath = info.getSourcePath().toString().substring(projectDir.toString().length());
        lastModified = info.getLastModified();
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

    public OffsetDateTime getLastModified()
    {
        return lastModified;
    }
}

