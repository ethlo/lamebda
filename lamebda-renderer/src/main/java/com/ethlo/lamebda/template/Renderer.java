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
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.DelegatingLoader;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import jdk.nashorn.internal.runtime.logging.DebugLogger;

public class Renderer
{
    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

    private PebbleEngine engine;

    public Renderer(Path baseTplPath)
    {
        //classpathLoader.setPrefix("lamebda/templates/");

        final FileLoader fileLoader = new FileLoader();
        fileLoader.setPrefix(baseTplPath.toAbsolutePath().toString());

        final List<Loader<?>> loaders = Arrays.asList(fileLoader); //, classpathLoader);

        engine = new PebbleEngine.Builder().templateCache(null).loader(new DelegatingLoader(loaders)).build();
    }

    public String render(String templateName, Map<String, Serializable> context)
    {
        logger.info("Rendering template {}: {}", templateName, context);
        final Writer writer = new StringWriter();
        try
        {
            final PebbleTemplate compiledTemplate = engine.getTemplate(templateName);
            compiledTemplate.evaluate(writer, wrap(context));
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

    private Map<String, Object> wrap(Map<String, Serializable> in)
    {
        final Map<String, Object> retVal = new TreeMap<>();
        in.forEach(retVal::put);
        return retVal;
    }
}
