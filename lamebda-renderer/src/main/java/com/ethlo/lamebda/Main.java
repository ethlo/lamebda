package com.ethlo.lamebda;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.ethlo.lamebda.template.Renderer;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

public class Main
{
    @Unmatched
    private List<String> unmatched;

    @Option(names = "-o", description = "The output for the rendered content", required = true)
    private Path outputFile;

    @Option(names = {"--tplName", "-i"}, description = "The input template name", required = true)
    private String tplName;

    @Option(names = {"--tplDir"}, description = "the template directory", required = true)
    private Path tplDir;

    @Option(names = {"--serialized-data"}, description = "the data to be applied ", required = true)
    private String serializedMapData;

    public static void main(String[] args) throws Exception
    {
        new Main(args);
    }

    public Main(String[] args) throws Exception
    {
        final CommandLine cmd = new CommandLine(this);
        try
        {
            cmd.parse(args);
        }
        catch (CommandLine.MissingParameterException exc)
        {
            System.err.println(exc.getMessage());
            CommandLine.usage(cmd, System.out);
            System.exit(1);
        }

        render();
    }

    private void render() throws IOException
    {
        final Map<String, Serializable> data = read(serializedMapData);
        final String result = new Renderer(tplDir).render(tplName, data);
        Files.write(outputFile, result.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Serializable> read(final String serializedMapData) throws IOException
    {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new HexBinaryAdapter().unmarshal(serializedMapData));
        final ObjectInputStream oin = new ObjectInputStream(bin);
        try
        {
            return (Map<String, Serializable>) oin.readObject();

        }
        catch (ClassNotFoundException e)
        {
            throw new UncheckedIOException(new IOException(e));
        }
    }
}
