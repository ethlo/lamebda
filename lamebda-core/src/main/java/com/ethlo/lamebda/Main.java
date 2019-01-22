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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.oas.ModelGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

public class Main
{
    @Unmatched
    private List<String> unmatched;

    @Option(names = "-m", description = "Create models")
    private boolean isCreateModels = true;

    @Option(names = {"-f", "--file"}, description = "the OAS specification file")
    private Path specificationFile = Paths.get(FileSystemLamebdaResourceLoader.SPECIFICATION_DIRECTORY, FileSystemLamebdaResourceLoader.API_SPECIFICATION_YAML_FILENAME);

    public static void main(String[] args) throws Exception
    {
        new Main(args);
    }

    public Main(String[] args) throws Exception
    {
        final CommandLine cmd = new CommandLine(this);
        cmd.parse(args);
        if (isCreateModels)
        {
            createModels(specificationFile.toAbsolutePath());
        }
        else
        {
            cmd.usage(System.err);
        }
    }

    private static void createModels(final Path specificationFile) throws IOException
    {
        if (Files.exists(specificationFile))
        {
            new ModelGenerator().generateModels(specificationFile);
        }
        else
        {
            System.err.println("File does not exist: " + specificationFile.toAbsolutePath());
        }
    }
}
