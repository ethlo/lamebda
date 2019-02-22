package com.ethlo.lamebda;

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
