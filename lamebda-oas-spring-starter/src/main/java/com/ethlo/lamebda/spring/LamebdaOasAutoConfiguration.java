package com.ethlo.lamebda.spring;

/*-
 * #%L
 * lamebda-oas-spring-starter
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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ethlo.lamebda.loaders.FunctionSourcePreProcessor;
import com.ethlo.lamebda.oas.ModelGenerator;

@Configuration
public class LamebdaOasAutoConfiguration
{
    @Bean
    public FunctionSourcePreProcessor functionLoadPreNotification()
    {
        return ((classLoader, sourcePath) -> {
            final Path specPath = Paths.get(Paths.get(sourcePath).getParent().toString(), "specification", "oas.yaml");
            if (specPath.toFile().exists())
            {
                try
                {
                    new ModelGenerator().generateModels(specPath.toString(), sourcePath, classLoader);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            return null;
        });
    }
}
