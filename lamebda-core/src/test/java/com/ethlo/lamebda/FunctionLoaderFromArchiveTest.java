package com.ethlo.lamebda;

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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.Test;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.util.IoUtil;

public class FunctionLoaderFromArchiveTest
{
    public FunctionLoaderFromArchiveTest() throws IOException
    {
    }

    @Test
    public void testLoadFromZip() throws Exception
    {
        final Path projectPath = Paths.get("/home/morten/core-lamebda/rfc");
        final ProjectConfiguration cfg = ProjectConfiguration.builder("lamebda", projectPath).listenForChanges(false).build();
        final FunctionManager functionManager = new FunctionManagerImpl(new FileSystemLamebdaResourceLoader(cfg, f -> f));
    }
}
