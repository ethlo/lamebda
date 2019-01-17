package com.ethlo.lamebda.functions;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ethlo.lamebda.FunctionResult;
import com.ethlo.lamebda.HttpMimeType;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.FileNameUtil;

public class StaticResourceFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private final Path resourceBasePath;

    public StaticResourceFunction(String prefix, Path resourceBasePath)
    {
        super("/" + prefix + "/**");
        Assert.notNull(resourceBasePath, "resourceBasePath cannot ne null");
        this.resourceBasePath = resourceBasePath;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response)
    {
        final String requestedPath = Paths.get(request.path()).normalize().toString().substring(pattern.length() - 2);
        final Path requestedFile = resourceBasePath.resolve(requestedPath).toAbsolutePath();
        if (isSubDirectory(resourceBasePath, requestedFile))
        {
            try
            {
                response.setContentType(HttpMimeType.fromExtension(FileNameUtil.getExtension(requestedFile.getFileName().toString())));
                final byte[] content = Files.readAllBytes(requestedFile);
                response.write(content);
            }
            catch (IOException e)
            {
                response.error(ErrorResponse.notFound(request.path()));
            }
        }
        else
        {
            response.error(HttpStatus.FORBIDDEN, "Nice try");
        }
    }

    private boolean isSubDirectory(Path base, Path child)
    {
        return child.toAbsolutePath().startsWith(base.toAbsolutePath());
    }
}
