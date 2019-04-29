package com.ethlo.lamebda.compiler.java;

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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;

import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PackageInternalsFinder
{
    private static final Logger logger = LoggerFactory.getLogger(PackageInternalsFinder.class);

    private final ClassLoader classLoader;
    private static final String CLASS_FILE_EXTENSION = ".class";

    public PackageInternalsFinder(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    public List<JavaFileObject> find(String packageName) throws IOException
    {
        final String javaPackageName = packageName.replaceAll("\\.", "/");
        final List<JavaFileObject> result = new ArrayList<>();
        final Enumeration<URL> urlEnumeration = classLoader.getResources(javaPackageName);
        while (urlEnumeration.hasMoreElements())
        {
            URL packageFolderURL = urlEnumeration.nextElement();
            result.addAll(listUnder(packageName, packageFolderURL));
        }

        return result;
    }

    private Collection<JavaFileObject> listUnder(String packageName, URL packageFolderURL)
    {
        final File directory = new File(packageFolderURL.getFile());
        if (directory.isDirectory())
        {
            return processDir(packageName, directory);
        }
        else
        {
            return processJar(packageFolderURL);
        }
    }

    private List<JavaFileObject> processJar(URL packageFolderURL)
    {
        final List<JavaFileObject> result = new ArrayList<>();
        try
        {
            final String ext = packageFolderURL.toExternalForm();
            final int idx = ext.lastIndexOf('!');
            final String jarUri = ext.substring(0, idx);
            logger.trace("Jar URI: {}", jarUri);

            final JarURLConnection jarConn = (JarURLConnection) packageFolderURL.openConnection();
            final String rootEntryName = jarConn.getEntryName();
            final int rootEnd = rootEntryName.length() + 1;

            final Enumeration<JarEntry> entryEnum = jarConn.getJarFile().entries();
            while (entryEnum.hasMoreElements())
            {
                final JarEntry jarEntry = entryEnum.nextElement();
                final String name = jarEntry.getName();
                if (name.startsWith(rootEntryName) && name.indexOf('/', rootEnd) == -1 && name.endsWith(CLASS_FILE_EXTENSION))
                {
                    final URI uri = URI.create(jarUri + "!/" + name);
                    String binaryName = name.replaceAll("/", ".");
                    binaryName = binaryName.replaceAll(CLASS_FILE_EXTENSION + "$", "");
                    result.add(new CustomJavaFileObject(binaryName, uri));
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Unable to open " + packageFolderURL + " as a jar file", e);
        }
        return result;
    }

    private List<JavaFileObject> processDir(String packageName, File directory)
    {
        final List<JavaFileObject> result = new ArrayList<>();
        final File[] childFiles = directory.listFiles();
        if (childFiles != null)
        {
            for (File childFile : childFiles)
            {
                if (childFile.isFile() && childFile.getName().endsWith(CLASS_FILE_EXTENSION))
                {
                    String binaryName = packageName + "." + childFile.getName();
                    binaryName = binaryName.replaceAll(CLASS_FILE_EXTENSION + "$", "");
                    result.add(new CustomJavaFileObject(binaryName, childFile.toURI()));
                }
            }
        }
        return result;
    }
}