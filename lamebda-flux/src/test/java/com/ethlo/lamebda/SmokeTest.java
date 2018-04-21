package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-flux
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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.ethlo.lamebda.loaders.FileSystemClassResourceLoader;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=TestCfg.class)
public class SmokeTest
{
    private static final Logger logger = LoggerFactory.getLogger(SmokeTest.class);
    
    @Autowired
    private ApplicationContext applicationContext;

    private FunctionManager functionManager;
    
    @Before
    public void setup() throws IOException
    {
        this.functionManager = null; //new FunctionManager(new FileSystemClassResourceLoader(applicationContext, "/Users/mha/Documents/ethlo/lamebda/src/test/resources/groovy/acme"));
    }
    
    @Test
    public void test() throws IOException
    {
        /*
        // This is what should be possible to perform at run-time
        new Thread()
        {
            public void run()
            {
                logger.info("Waiting to add function");
                try
                {
                    Thread.sleep(5_000);
                }
                catch (InterruptedException exc)
                {
                    Thread.currentThread().interrupt();
                }
        
                logger.info("Adding function");
                final RoutedHandlerFunction func = classResourceLoader.loadClass("FunctionA");
                functionManager.addFunction(func);
            }
        }.start();
        
        new GatewayServer(functionManager, "localhost", 10500);
        */
    }
}
