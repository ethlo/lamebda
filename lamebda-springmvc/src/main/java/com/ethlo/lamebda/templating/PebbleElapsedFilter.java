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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

public class PebbleElapsedFilter implements Filter
{
    @Override
    public Object apply(final Object input, final Map<String, Object> args, final PebbleTemplate self, final EvaluationContext context, final int lineNumber) throws PebbleException
    {
        return humanReadableFormat(Duration.ofSeconds(Instant.now().toEpochMilli() / 1000).minusSeconds(((OffsetDateTime) input).toEpochSecond()));
    }

    @Override
    public List<String> getArgumentNames()
    {
        return Collections.emptyList();
    }

    public static String humanReadableFormat(Duration duration)
    {
        if (duration.toDays() == 0)
        {
            return String.format("%02d:%02d:%02d",
                    duration.toHours(),
                    duration.toMinutes() - TimeUnit.HOURS.toMinutes(duration.toHours()),
                    duration.getSeconds() - TimeUnit.MINUTES.toSeconds(duration.toMinutes())
            );
        }
        final String dayPlurality = duration.toDays() == 1 ? "" : "s";
        return String.format("%s day%s, %02d:%02d:%02d", duration.toDays(), dayPlurality,
                duration.toHours() - TimeUnit.DAYS.toHours(duration.toDays()),
                duration.toMinutes() - TimeUnit.HOURS.toMinutes(duration.toHours()),
                duration.getSeconds() - TimeUnit.MINUTES.toSeconds(duration.toMinutes())
        );
    }
}
