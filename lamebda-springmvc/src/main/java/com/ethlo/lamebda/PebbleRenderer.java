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

import com.ethlo.lamebda.templating.PebbleElapsedFilter;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;

public class PebbleRenderer
{
    private final PebbleEngine engine;

    public PebbleRenderer(final String basePath, boolean strict)
    {
        final Map<String, Filter> filters = new TreeMap<>();
        filters.put("elapsed", new PebbleElapsedFilter());

        final Loader<String> loader = getLoader(basePath);
        engine = new PebbleEngine.Builder()
                .loader(loader)
                .cacheActive(loader instanceof ClasspathLoader)
                .strictVariables(strict)
                .extension(new AbstractExtension()
                {
                    @Override
                    public Map<String, Filter> getFilters()
                    {
                        return filters;
                    }
                }).build();
    }

    private Loader<String> getLoader(final String basePath)
    {
        if (basePath.startsWith("classpath:"))
        {
            final ClasspathLoader classpathLoader = new ClasspathLoader();
            classpathLoader.setPrefix(basePath.substring(10));
            classpathLoader.setSuffix(".tpl.html");
            return classpathLoader;
        }
        if (basePath.startsWith("file://"))
        {
            final FileLoader fileLoader = new FileLoader();
            fileLoader.setPrefix(basePath.substring(7));
            fileLoader.setSuffix(".tpl.html");
            return fileLoader;
        }
        else
        {
            throw new IllegalArgumentException("Unknown scheme: " + basePath);
        }
    }

    public String render(Map<String, Object> data, String tplName, Locale locale)
    {
        final StringWriter sw = new StringWriter();
        try
        {
            engine.getTemplate(tplName).evaluate(sw, data, locale);
            return sw.toString();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
