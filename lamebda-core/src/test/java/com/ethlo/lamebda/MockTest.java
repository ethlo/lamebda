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
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;

@RunWith(MockitoJUnitRunner.class)
@PropertyFile("src/test/resources/config.properties")
public class MockTest
{
    public MockTest() throws IOException
    {
    }

    @Spy
    private final FunctionContext context = new FileSystemLamebdaResourceLoader(ProjectConfiguration.builder("test", Paths.get(""))
            .loadIfExists()
            .build())
            .loadContext(MockTest.class);

    @Test
    public void testInit()
    {

    }
}
