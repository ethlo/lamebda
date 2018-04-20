package com.ethlo.lamebda.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

public class IoUtil
{
    private IoUtil(){}
    
    public static byte[] toByteArray(final InputStream in)
    {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        
        try
        {
            while ((nRead = in.read(data, 0, data.length)) != -1)
            {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        }
        catch (IOException exc)
        {
            throw new UncheckedIOException(exc);
        }
        return buffer.toByteArray();
    }
    
    public static String toString(InputStream in, Charset charset)
    {
        return new String(toByteArray(in), charset);
    }

    public static String classPathResourceAsString(String path, Charset charset)
    {
        final InputStream in = IoUtil.class.getClassLoader().getResourceAsStream(path);
        if (in != null)
        {
            return toString(in, charset);
        }
        throw new UncheckedIOException(new FileNotFoundException(path));
    }
}
