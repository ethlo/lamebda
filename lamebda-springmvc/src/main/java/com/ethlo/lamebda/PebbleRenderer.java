package com.ethlo.lamebda;

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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.loader.StringLoader;

public class PebbleRenderer
{
    private final PebbleEngine engine;

    public PebbleRenderer(boolean strict)
    {
        final Map<String, Filter> filters = new TreeMap<>();
        engine = new PebbleEngine.Builder()
                .strictVariables(strict)
                .loader(new StringLoader())
                .extension(new AbstractExtension()
                {
                    @Override
                    public Map<String, Filter> getFilters()
                    {
                        return filters;
                    }
                }).build();
    }

    public String render(Map<String, Object> data, String message, Locale locale)
    {
        final StringWriter sw = new StringWriter();
        try
        {
            engine.getTemplate(message).evaluate(sw, data, locale);
            return sw.toString();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
