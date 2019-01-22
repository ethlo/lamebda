package com.ethlo.lamebda.functions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import com.ethlo.lamebda.HttpMimeType;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.util.FileNameUtil;
import com.ethlo.lamebda.util.IoUtil;

public class DirectoryResourceFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private final Path basePath;
    private String basePattern;

    public DirectoryResourceFunction(final String prefix, final Path basePath)
    {
        super(prefix + "/**");
        this.basePath = basePath.toAbsolutePath();
    }

    @Override
    public void init(final ProjectConfiguration projectConfiguration)
    {
        basePattern = Paths.get("/" + projectConfiguration.getContextPath() + "/" + StringUtils.strip(pattern, "*/")).normalize().toString();
    }

    @Override
    protected void get(final HttpRequest request, final HttpResponse response)
    {
        final String requestedUrlPath = Paths.get(request.path()).normalize().toString();
        final String subResource = requestedUrlPath.substring(basePattern.length());
        final Path requestedPath = Paths.get(basePath.toString(), subResource).toAbsolutePath();

        if (!requestedPath.toString().startsWith(basePath.toString()))
        {
            response.error(new ErrorResponse(HttpStatus.FORBIDDEN, "Resources outside the defined base is not allowed"));
            return;
        }

        if (Files.isDirectory(requestedPath))
        {
            final Path indexFile = requestedPath.resolve("index.html");
            if (Files.exists(indexFile))
            {
                response.setContentType(HttpMimeType.HTML);
                response.write(IoUtil.toByteArray(indexFile));
                return;
            }
            response.error(new ErrorResponse(HttpStatus.FORBIDDEN, "Directory listing denied"));
            return;
        }

        try
        {
            if (Files.isRegularFile(requestedPath) && !Files.isHidden(requestedPath))
            {
                final String ext = FileNameUtil.getExtension(requestedPath.getFileName().toString());
                response.setContentType(HttpMimeType.fromExtension(ext));
                response.write(IoUtil.toByteArray(requestedPath));
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
