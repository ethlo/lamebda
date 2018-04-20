package com.ethlo.lamebda.util;

public class Assert
{
    private Assert(){}

    public static <T> T notNull(T o, String message)
    {
        if (o == null)
        {
            throw new IllegalArgumentException(message);
        }
        return o;
    }

    public static void isTrue(boolean equals, String message)
    {
        if (! equals)
        {
            throw new IllegalArgumentException(message); 
        }
    }
}
