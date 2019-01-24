package com.ethlo.lamebda.generator;

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
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class GeneratorHelper
{
    private final Path generatorCliJarPath;

    public GeneratorHelper(final Path generatorCliJarPath)
    {
        this.generatorCliJarPath = generatorCliJarPath;
    }

    public URL generateModels(final Path specificationFile, final Path target) throws IOException
    {
        Process process = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-jar", generatorCliJarPath.toAbsolutePath().toString(), "generate",  "-i",  specificationFile.toAbsolutePath().toString(), "-g",  "jaxrs-spec",  "-o", target.toAbsolutePath().toString(), "-Dmodel",  "-DdateLibrary=java8",  "--model-package=spec", "-DuseSwaggerAnnotations=false")
                .inheritIO()
                .start();
        try
        {
            if (! process.waitFor(30_000, TimeUnit.MILLISECONDS))
            {
                throw new IOException("Execution failed");
            }
        }
        catch (InterruptedException e)
        {
            e.notifyAll();
        }

        return target.toUri().toURL();
    }

    public void generateApiDoc(final Path specificationFile, final Path target)
    {

    }
}
