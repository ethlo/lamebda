package com.ethlo.lamebda.compiler;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author atamur
 * @since 15-Oct-2009
 */
public class CustomClassloaderJavaFileManager implements JavaFileManager
{
    private static final Logger logger = LoggerFactory.getLogger(CustomClassloaderJavaFileManager.class);

    private final ClassLoader classLoader;
    private final StandardJavaFileManager standardFileManager;
    private final PackageInternalsFinder finder;

    public CustomClassloaderJavaFileManager(ClassLoader classLoader, StandardJavaFileManager standardFileManager)
    {
        this.classLoader = classLoader;
        this.standardFileManager = standardFileManager;
        finder = new PackageInternalsFinder(classLoader);
    }

    @Override
    public ClassLoader getClassLoader(Location location)
    {
        return classLoader;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file)
    {
        if (file instanceof CustomJavaFileObject)
        {
            return ((CustomJavaFileObject) file).binaryName();
        }
        else
        {
            return standardFileManager.inferBinaryName(location, file);
        }
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b)
    {
        return standardFileManager.isSameFile(a, b);
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining)
    {
        return standardFileManager.handleOption(current, remaining);
    }

    @Override
    public boolean hasLocation(Location location)
    {
        return location == StandardLocation.CLASS_PATH || location == StandardLocation.PLATFORM_CLASS_PATH;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException
    {
        return standardFileManager.getJavaFileForInput(location, className, kind);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException
    {
        return standardFileManager.getJavaFileForOutput(location, className, kind, sibling);
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException
    {
        return standardFileManager.getFileForInput(location, packageName, relativeName);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException
    {
        return standardFileManager.getFileForOutput(location, packageName, relativeName, sibling);
    }

    @Override
    public void flush() throws IOException
    {
        standardFileManager.flush();
    }

    @Override
    public void close() throws IOException
    {
        standardFileManager.close();
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
    {
        logger.trace("Lookup package name: {}", packageName);

        if (location == StandardLocation.PLATFORM_CLASS_PATH)
        {
            return standardFileManager.list(location, packageName, kinds, recurse);
        }
        else if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS))
        {
            final Iterable<JavaFileObject> stdClasses = standardFileManager.list(location, packageName, kinds, recurse);
            final Iterable<JavaFileObject> appClasses = finder.find(packageName);
            final List<JavaFileObject> joined = new LinkedList<>();
            stdClasses.forEach(joined::add);
            appClasses.forEach(joined::add);
            return joined;
        }
        return Collections.emptyList();

    }

    @Override
    public int isSupportedOption(String option)
    {
        return standardFileManager.isSupportedOption(option);
    }
}
