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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.IoUtil;
import com.ethlo.lamebda.util.StringUtil;

public abstract class BaseExecHelper
{
    private final String javaCmd;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<String> classPath;

    public BaseExecHelper(final String javaCmd, final Path jarPath)
    {
        Assert.isTrue(Files.exists(Paths.get(javaCmd)), "Java command " + javaCmd + " does not exist");
        this.javaCmd = javaCmd;
        Assert.isTrue(Files.exists(jarPath), "JAR directory" + jarPath + " does not exist");
        this.classPath = IoUtil.toClassPathList(jarPath).stream().map(u -> u.getPath()).collect(Collectors.toList());
    }

    protected int doExec(Path dir, String... cmd) throws IOException
    {
        return this.doExec(dir, cmd, Duration.ofSeconds(30));
    }

    protected int doExec(Path dir, String[] cmd, Duration timeout) throws IOException
    {
        Process process = null;

        try
        {
            final String[] fullCmd = combine(new String[]{javaCmd, "-cp",  StringUtil.join(classPath, File.pathSeparator), "org.openapitools.codegen.OpenAPIGenerator"}, cmd);
            logger.debug("Running {}", StringUtil.join(Arrays.asList(fullCmd), " "));
            process = new ProcessBuilder(fullCmd)
                    .inheritIO()
                    .directory(dir.toFile())
                    .start();
            try
            {
                process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                return process.exitValue();
            }
            catch (InterruptedException e)
            {
                e.notifyAll();
            }
        }
        finally
        {
            if (process != null)
            {
                process.destroy();
            }
        }
        return process.exitValue();
    }

    private String[] combine(final String[] a, final String[] b)
    {
        final String[] combined = new String[a.length + b.length];
        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return combined;
    }
}
