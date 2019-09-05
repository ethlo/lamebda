package com.ethlo.lamebda.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

public class HttpUtil
{
    public static Resource getContent(String urlStr)
    {
        final URL url = IoUtil.stringToURL(urlStr);
        final String userInfo = url.getUserInfo();
        if (userInfo != null)
        {
            final String[] usernameAndPassword = userInfo.split(":");
            final String normalizedUrl = normalize(url);
            return HttpUtil.getContent(normalizedUrl, usernameAndPassword[0], usernameAndPassword[1]);
        }
        return HttpUtil.getContent(urlStr, null, null);
    }

    public static String normalize(final URL url)
    {
        return url.getProtocol() + "://" + url.getHost()
                        + (url.getPort() != -1 ? ":" + url.getPort() : "")
                        + url.getPath();
    }

    public static Resource getContent(String url, String username, String password)
    {
        try
        {
            final URLConnection connection = new URL(url).openConnection();
            if (username != null && password != null)
            {
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
            }
            try (final InputStream in = new BufferedInputStream(connection.getInputStream()))
            {
                final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                StreamUtils.copy(in, bout);
                return new ByteArrayResource(bout.toByteArray(), url);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
