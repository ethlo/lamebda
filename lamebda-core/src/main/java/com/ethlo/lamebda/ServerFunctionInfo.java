package com.ethlo.lamebda;

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

import java.time.ZonedDateTime;

public class ServerFunctionInfo
{
    private final String sourcePath;
    private ZonedDateTime lastModified;
    
    public ServerFunctionInfo(String sourcePath)
    {
        this.sourcePath = sourcePath;
    }

    public ZonedDateTime getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(ZonedDateTime lastModified)
    {
        this.lastModified = lastModified;
    }

    public String getSourcePath()
    {
        return sourcePath;
    }
}
        