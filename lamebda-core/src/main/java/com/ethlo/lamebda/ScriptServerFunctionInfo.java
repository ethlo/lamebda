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

import java.nio.file.Path;
import java.time.OffsetDateTime;

import com.ethlo.lamebda.loaders.LamebdaResourceLoader;

public class ScriptServerFunctionInfo extends AbstractServerFunctionInfo
{
    private final Class<? extends ServerFunction> type;
    private final OffsetDateTime lastModified;

    public ScriptServerFunctionInfo(Class<? extends ServerFunction> type,  OffsetDateTime lastModified)
    {
        super(type.getCanonicalName());
        this.type = type;
        this.lastModified = lastModified;
    }

    @Override
    public Class<? extends ServerFunction> getType()
    {
        return type;
    }

    public OffsetDateTime getLastModified()
    {
        return lastModified;
    }
}
        