package com.ethlo.lamebda;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestCfg
{
    @Bean
    public FakeService fakeService()
    {
        return new FakeService();
    }
}