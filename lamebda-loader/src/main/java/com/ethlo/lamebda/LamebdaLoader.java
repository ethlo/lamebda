package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-loader
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LamebdaLoader
{
    private final Object instance;

    public LamebdaLoader() throws IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException
    {
        // Dependencies
        final Path libDir = Paths.get("/home/morten/development/lamebda/lamebda-core/target/dependencies");
        final List<URL> all = Files.list(libDir)
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .map(this::pathToUrl).collect(Collectors.toList());


        // Lamebda libs
        all.add(pathToUrl(Paths.get("/home/morten/development/lamebda/lamebda-core/target/lamebda-core-0.5.4-SNAPSHOT.jar")));

        final ParentLastURLClassLoader cl = new ParentLastURLClassLoader(all);

        final String className = getClass().getPackage().getName() + ".FunctionManagerDirector";
        final Class<?> clazz = cl.loadClass(className, true);

        final Path rootDirectory = Paths.get("/var/lib/lamebda");
        final String rootContext = "/gateway";

        final Constructor<?> constructor = clazz.getConstructor(Path.class, String.class);

        this.instance = constructor.newInstance(rootDirectory, rootContext);
        System.out.println(instance);
    }

    public Object getInstance()
    {
        return instance;
    }

    private URL pathToUrl(final Path p)
    {
        System.out.println(p.getFileName());
        if (!Files.exists(p))
        {
            throw new UncheckedIOException(new FileNotFoundException(p.toAbsolutePath().toString()));
        }

        try
        {
            return URI.create("file://" + p.toAbsolutePath().toString()).toURL();
        }
        catch (MalformedURLException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) throws Exception
    {
        new LamebdaLoader();
    }
}
