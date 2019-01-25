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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class RendererExec extends BaseExecHelper
{
    private Path templateDirectory;

    public RendererExec(final String javaCmd, final Path jarPath, final Path templateDirectory)
    {
        super(javaCmd, jarPath);
        this.templateDirectory = templateDirectory;
    }

    public void render(String tplName, Map<String, Serializable> data, Path targetFile) throws IOException
    {
        doExec("--tplName", tplName, "--tplDir", templateDirectory.toAbsolutePath().toString(), "--serialized-data", encode(data), "-o", targetFile.toAbsolutePath().toString());
    }

    private String encode(final Map<String, Serializable> data) throws IOException
    {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(data);
        return new HexBinaryAdapter().marshal(bout.toByteArray());
    }
}
