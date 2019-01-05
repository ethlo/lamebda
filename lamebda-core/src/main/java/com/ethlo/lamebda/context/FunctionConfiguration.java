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

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Properties;

import com.ethlo.time.ITU;

public class FunctionConfiguration extends Properties
{
    public Integer getInt(String key)
    {
        final String s = getString(key);
        return s != null ? Integer.parseInt(s) : null;
    }

    public Long getLong(String key)
    {
        final String s = getString(key);
        return s != null ? Long.parseLong(s) : null;
    }

    public OffsetDateTime getDateTime(String key)
    {
        final String s = getString(key);
        return s != null ? ITU.parseDateTime(s) : null;
    }

    public Temporal getDate(String key)
    {
        final String s = getString(key);
        return s != null ? ITU.parseLenient(s) : null;
    }

    public String getString(String key)
    {
        return (String)get(key);
    }
}
