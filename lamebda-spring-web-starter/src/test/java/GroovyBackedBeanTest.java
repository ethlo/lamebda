/*-
 * #%L
 * lamebda-spring-web-starter
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
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.ethlo.lamebda.ConfigurableFunctionManager;
import com.ethlo.lamebda.FunctionManagerDirector;
import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.ServerFunction;
import com.ethlo.lamebda.loaders.FileSystemLamebdaResourceLoader;
import com.ethlo.lamebda.spring.LamebdaSpringWebAutoConfiguration;

@SpringBootTest(classes = LamebdaSpringWebAutoConfiguration.class)
@RunWith(SpringRunner.class)
public class GroovyBackedBeanTest
{
    @Autowired
    private FunctionManagerDirector functionManagerDirector;

    @Test
    public void init()
    {
        final Path basePath = Paths.get("src/test/groovy/test");
        final FunctionManagerImpl fm = (FunctionManagerImpl) functionManagerDirector.getFunctionManagers().get(basePath);
        assertThat(fm).isNotNull();
        assertThat(fm.getFunctions()).containsKey("mycontrollers.Correct");
    }
}
