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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.ethlo.lamebda.context.FunctionConfiguration;

public class FunctionConfigurationTest
{
    private final FunctionConfiguration cfg = new FunctionConfiguration();

    @Test
    public void testExistingWithDefault()
    {
        cfg.setProperty("exists2", "2");
        assertThat(cfg.getInt("exists2", 5)).isEqualTo(2);
    }

    @Test
    public void testExistingWithoutDefault()
    {
        cfg.setProperty("exists1", "1");
        assertThat(cfg.getInt("exists1")).isEqualTo(1);
    }

    @Test
    public void testMissingWithDefault()
    {
        assertThat(cfg.getInt("foo", 123)).isEqualTo(123);
    }

    @Test
    public void testMissingWithNullDefault()
    {
        assertThat(cfg.getInt("foo", null)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingWithoutDefault()
    {
        cfg.getInt("foo");
    }
}
