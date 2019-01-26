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
import java.io.UncheckedIOException;
import java.nio.file.Path;

public class GeneratorHelper extends BaseExecHelper
{
    public GeneratorHelper(final String javaCmd, final Path jarDirectory)
    {
        super(javaCmd, jarDirectory);
    }

    public void generate(Path dir, String... args) throws IOException
    {
        final int exitCode = doExec(dir, args);
        if (exitCode != 0)
        {
            throw new UncheckedIOException(new IOException("Generator returrned exit code " + exitCode));
        }
    }
}
