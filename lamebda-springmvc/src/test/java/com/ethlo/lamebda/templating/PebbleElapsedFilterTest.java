package com.ethlo.lamebda.templating;

/*-
 * #%L
 * lamebda-springmvc
 * %%
 * Copyright (C) 2018 - 2022 Morten Haraldsen (ethlo)
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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PebbleElapsedFilterTest
{
    @Test
    void testSubDay()
    {
        final Duration d = Duration.parse("PT14h16m59s");
        assertThat(PebbleElapsedFilter.humanReadableFormat(d)).isEqualTo("14:16:59");
    }

    @Test
    void testOneDay()
    {
        final Duration d = Duration.parse("P1dT14h16m59s");
        assertThat(PebbleElapsedFilter.humanReadableFormat(d)).isEqualTo("1 day, 14:16:59");
    }

    @Test
    void testMultipleDays()
    {
        final Duration d = Duration.parse("P6dT14h16m59s");
        assertThat(PebbleElapsedFilter.humanReadableFormat(d)).isEqualTo("6 days, 14:16:59");
    }
}
