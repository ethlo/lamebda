package com.ethlo.lamebda;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.ethlo.lamebda.context.FunctionConfiguration;

public class FunctionConfigurationTest
{
    private final FunctionConfiguration cfg = new FunctionConfiguration();

    @Test
    public void testExistingWithDefault()
    {
        cfg.setProperty("exists2", "2");
        assertThat(cfg.getInt("exists2", 5)).isEqualTo(2);
    }

    @Test
    public void testExistingWithoutDefault()
    {
        cfg.setProperty("exists1", "1");
        assertThat(cfg.getInt("exists1")).isEqualTo(1);
    }

    @Test
    public void testMissingWithDefault()
    {
        assertThat(cfg.getInt("foo", 123)).isEqualTo(123);
    }

    @Test
    public void testMissingWithNullDefault()
    {
        assertThat(cfg.getInt("foo", null)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingWithoutDefault()
    {
        cfg.getInt("foo");
    }
}
