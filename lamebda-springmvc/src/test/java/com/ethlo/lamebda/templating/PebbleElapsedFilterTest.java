package com.ethlo.lamebda.templating;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PebbleElapsedFilterTest
{
    @Test
    void testSubDay()
    {
        final Duration d = Duration.parse("PT14h16m59s");
        assertThat(PebbleElapsedFilter.humanReadableFormat(d)).isEqualTo("14:16:59");
    }

    @Test
    void testOneDay()
    {
        final Duration d = Duration.parse("P1dT14h16m59s");
        assertThat(PebbleElapsedFilter.humanReadableFormat(d)).isEqualTo("1 day, 14:16:59");
    }

    @Test
    void testMultipleDays()
    {
        final Duration d = Duration.parse("P6dT14h16m59s");
        assertThat(PebbleElapsedFilter.humanReadableFormat(d)).isEqualTo("6 days, 14:16:59");
    }
}