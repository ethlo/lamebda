package com.ethlo.lamebda.template;

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

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.LoaderException;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.DelegatingLoader;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

public class Renderer
{
    private PebbleEngine engine;

    public Renderer(Path baseTplPath)
    {
        final ClasspathLoader classpathLoader = new ClasspathLoader();
        classpathLoader.setPrefix("lamebda/templates/");

        final FileLoader fileLoader = new FileLoader();
        fileLoader.setPrefix(baseTplPath.toAbsolutePath().toString());

        engine = new PebbleEngine();
        engine.setTemplateCache(null);
        final List<Loader> loaders = Arrays.asList(fileLoader, classpathLoader);
        engine.setLoader(new DelegatingLoader(loaders)
        {
            @Override
            public Reader getReader(final String templateName) throws LoaderException
            {
                for (Loader loader : loaders)
                {
                    try
                    {
                        return loader.getReader(templateName);
                    }
                    catch (LoaderException e)
                    {
                        // do nothing
                    }
                }
                throw new LoaderException(null, "Could not find template \"" + templateName + "\"");
            }
        });
    }

    public String render(String templateName, Map<String, Object> context)
    {
        final Writer writer = new StringWriter();
        try
        {
            final PebbleTemplate compiledTemplate = engine.getTemplate(templateName);
            compiledTemplate.evaluate(writer, context);
            return writer.toString();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (PebbleException e)
        {
            throw new UncheckedPebbleException(e);
        }
    }
}
