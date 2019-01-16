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
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import com.ethlo.time.ITU;

public class FunctionConfiguration extends Properties
{
    public Integer getInt(String key)
    {
        return assertNotNull(key, Integer::parseInt, null);
    }

    public Integer getInt(String key, Integer defaultValue)
    {
        return assertNotNull(key, Integer::parseInt, Optional.ofNullable(defaultValue));
    }

    public Long getLong(String key)
    {
        return assertNotNull(key, Long::parseLong, null);
    }

    public Long getLong(String key, Long defaultValue)
    {
        return assertNotNull(key, Long::parseLong, Optional.ofNullable(defaultValue));
    }

    public OffsetDateTime getDateTime(String key)
    {
        return assertNotNull(key, ITU::parseDateTime, null);
    }

    public OffsetDateTime getDateTime(String key, OffsetDateTime defaultValue)
    {
        return assertNotNull(key, ITU::parseDateTime, Optional.ofNullable(defaultValue));
    }

    public Temporal getDate(String key)
    {
        return assertNotNull(key, ITU::parseLenient, null);
    }

    public Temporal getDate(String key, Temporal defaultValue)
    {
        return assertNotNull(key, ITU::parseLenient, Optional.ofNullable(defaultValue));
    }

    public String getString(String key)
    {
        return assertNotNull(key, i -> i, Optional.empty());
    }

    private <T> T assertNotNull(final String key, Function<String, T> converter, Optional<T> defaultValue)
    {
        final String s = getProperty(key);
        if (s != null)
        {
            return converter.apply(s);
        }
        else if (defaultValue == null)
        {
            throw new IllegalArgumentException("No value for configuration key " + key);
        }
        return defaultValue.orElse(null);
    }
}
