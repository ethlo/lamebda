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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ethlo.lamebda.util.IoUtil;

@ExtendWith(SpringExtension.class)
public abstract class BaseTest
{
    private final Path rootPath = Paths.get("src/test/projects");
    private final Path projectPath = rootPath.resolve("myproject");
    protected ProjectImpl project;

    @Autowired
    private ApplicationContext parentContext;

    @BeforeEach
    public void setup()
    {
        deleteTarget();
        final Properties properties = new Properties();
        properties.put("project.name", "my-test-project");
        properties.put("project.source.java", "target/generated-sources/java");
        properties.put("project.base-packages", "acme");
        final BootstrapConfiguration cfg = new BootstrapConfiguration("/gateway", projectPath, properties);
        project = new ProjectImpl(parentContext, cfg, ProjectManager.setupWorkDir(projectPath));
    }

    private void deleteTarget()
    {
        try
        {
            IoUtil.deleteDirectory(projectPath.resolve("target"));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
