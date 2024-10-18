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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Valid
public class ProjectInfo
{
    private String name;

    private String version;

    @NotEmpty
    private Set<String> basePackages = new LinkedHashSet<>();

    public String getName()
    {
        return name;
    }

    public ProjectInfo setName(final String name)
    {
        this.name = name;
        return this;
    }

    public String getVersion()
    {
        return version;
    }

    public ProjectInfo setVersion(final String version)
    {
        this.version = version;
        return this;
    }

    public Set<String> getBasePackages()
    {
        return Optional.ofNullable(basePackages).orElseThrow(() -> new IllegalArgumentException("No base-packages set to scan"));
    }

    public ProjectInfo setBasePackages(final Set<String> basePackages)
    {
        this.basePackages = basePackages;
        return this;
    }
}
