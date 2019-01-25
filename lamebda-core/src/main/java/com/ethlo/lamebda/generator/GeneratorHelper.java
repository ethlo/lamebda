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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ethlo.lamebda.util.IoUtil;
import sun.nio.ch.IOUtil;

public class GeneratorHelper extends BaseExecHelper
{
    public GeneratorHelper(final String javaCmd, final Path jarPath)
    {
        super(javaCmd, jarPath);
    }

    public URL generateModels(final Path specificationFile, final Path target, Path tplOverride) throws IOException
    {
        final List<String> cmd = new ArrayList<>(Arrays.asList("generate",
                "-i",  specificationFile.toAbsolutePath().toString(), "-g",  "jaxrs-spec",
                "-o", target.toAbsolutePath().toString(), "-Dmodels",  "-DdateLibrary=java8",
                "--model-package=spec", "-DuseSwaggerAnnotations=false"));

        if (tplOverride != null)
        {
            cmd.add("-t");
            cmd.add(tplOverride.toAbsolutePath().toString());
        }

        doExec(cmd.toArray(new String[cmd.size()]));

        final Path modelDir = target.resolve("src/gen/java");
        IoUtil.changeExtension(modelDir, "java", "groovy");
        return modelDir.toUri().toURL();
    }

    public void generateApiDoc(final Path specificationFile, final Path target, Path tplOverride) throws IOException
    {
        final List<String> cmd = new ArrayList<>(Arrays.asList("generate",
                "-i",  specificationFile.toAbsolutePath().toString(), "-g",  "html",
                "-o", target.toAbsolutePath().toString()));
        if (tplOverride != null)
        {
            cmd.add("-t");
            cmd.add(tplOverride.toAbsolutePath().toString());
        }

        doExec(cmd.toArray(new String[cmd.size()]));
    }
}
