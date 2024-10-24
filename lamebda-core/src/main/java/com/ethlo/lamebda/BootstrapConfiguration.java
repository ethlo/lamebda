package com.ethlo.lamebda;

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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Properties;

import jakarta.validation.Valid;

@Valid
public class BootstrapConfiguration implements Serializable
{
    private final Path path;
    private final String rootContextPath;
    private final Properties envProperties;

    public BootstrapConfiguration(final String rootContextPath, final Path path, final Properties envProperties)
    {
        this.rootContextPath = rootContextPath;
        this.path = path.toAbsolutePath();
        this.envProperties = envProperties;
    }

    public Path getPath()
    {
        return path;
    }

    public String getRootContextPath()
    {
        return rootContextPath;
    }

    public Properties getEnvProperties()
    {
        return envProperties;
    }
}
