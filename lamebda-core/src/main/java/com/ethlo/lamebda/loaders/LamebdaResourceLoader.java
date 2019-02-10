package com.ethlo.lamebda.loaders;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 Morten Haraldsen (ethlo)
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
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.ethlo.lamebda.ApiSpecificationModificationNotice;
import com.ethlo.lamebda.FunctionModificationNotice;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.ServerFunctionInfo;
import com.ethlo.lamebda.io.FileSystemEvent;
import groovy.lang.GroovyClassLoader;

public interface LamebdaResourceLoader extends AutoCloseable
{
    /**
     * Load the contents of the class
     *
     * @param sourcePath The path to the source file
     * @return The contents of the specified name
     * @throws IOException If the class could not be found or loaded
     */
    String readSource(Path sourcePath) throws IOException;

    /**
     * Return a list of all known functions
     *
     * @param offset The number of items to skip
     * @param size   The number of items to return
     * @return A list of {@link ServerFunctionInfo}s
     */
    List<ServerFunctionInfo> findAll(long offset, int size);

    void setProjectChangeListener(Consumer<FileSystemEvent> l);

    void setApiSpecificationChangeListener(Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener);

    void setLibChangeListener(Consumer<FileSystemEvent> listener);

    void setFunctionChangeListener(Consumer<FunctionModificationNotice> l);

    ServerFunction load(GroovyClassLoader classLoader, Path sourcePath);

    Class<ServerFunction> loadClass(GroovyClassLoader classLoader, Path sourcePath);

    Optional<Path> getApiSpecification();

    URL getSharedClassPath();

    List<URL> getLibUrls();

    ProjectConfiguration getProjectConfiguration();

    void close();
}
