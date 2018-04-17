package com.ethlo.lamebda;

import java.util.Collections;

import org.springframework.stereotype.Service;

@Service
public class FakeService
{
    public Object getSomething()
    {
        return Collections.singletonMap("hello", "world");
    }
}