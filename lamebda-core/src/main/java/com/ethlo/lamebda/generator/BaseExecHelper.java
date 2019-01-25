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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.StringUtil;

public abstract class BaseExecHelper
{
    private final String javaCmd;
    private final Path jarPath;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public BaseExecHelper(final String javaCmd, final Path jarPath)
    {
        Assert.isTrue(Files.exists(Paths.get(javaCmd)), "Java command " + javaCmd + " does not exist");
        this.javaCmd = javaCmd;
        Assert.isTrue(Files.exists(jarPath), "JAR " + jarPath + " does not exist");
        this.jarPath = jarPath;
    }

    protected void doExec(String... cmd) throws IOException
    {
        this.doExec(cmd, Duration.ofSeconds(30));
    }

    protected void doExec(String[] cmd, Duration timeout) throws IOException
    {
        Process process = null;

        try
        {
            final String[] fullCmd = combine(new String[]{javaCmd, "-jar", jarPath.toAbsolutePath().toString()}, cmd);
            logger.info("Running {}", StringUtil.join(Arrays.asList(fullCmd), " "));
            process = new ProcessBuilder(fullCmd)
                    .inheritIO()
                    .start();
            try
            {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS))
                {
                    throw new IOException("Execution failed");
                }
            }
            catch (InterruptedException e)
            {
                e.notifyAll();
            }
        } finally
        {
            if (process != null)
            {
                process.destroy();
            }
        }
    }

    private String[] combine(final String[] a, final String[] b)
    {
        final String[] combined = new String[a.length + b.length];
        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return combined;
    }
}
