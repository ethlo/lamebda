package com.ethlo.lamebda;

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
