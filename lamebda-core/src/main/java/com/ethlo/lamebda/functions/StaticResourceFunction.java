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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ethlo.lamebda.FunctionResult;
import com.ethlo.lamebda.HttpMimeType;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.util.FileNameUtil;
import com.ethlo.lamebda.util.IoUtil;

public class StaticResourceFunction extends SimpleServerFunction
{
    private final File resourceBasePath;

    public StaticResourceFunction(File resourceBasePath)
    {
        super("/static/*");
        this.resourceBasePath = resourceBasePath;
    }

    @Override
    public FunctionResult handle(HttpRequest request, HttpResponse response)
    {
        final String requestedPath;
        try
        {
            requestedPath = getPathVars("/static/{resourcePath}", request).get("resourcePath");
        }
        catch (IllegalArgumentException exc)
        {
            return FunctionResult.SKIPPED;
        }

        final File requestedFile = Paths.get(resourceBasePath.getAbsolutePath(), requestedPath).toAbsolutePath().toFile();
        if (isSubDirectory(resourceBasePath, requestedFile))
        {
            try
            {
                response.setContentType(HttpMimeType.fromExtension(FileNameUtil.getExtension(requestedFile.getName())));
                final byte[] content = IoUtil.toByteArray(new FileInputStream(requestedFile));
                response.write(content);
            }
            catch (FileNotFoundException e)
            {
                response.error(ErrorResponse.notFound(request.path()));
            }
        }
        else
        {
            response.error(HttpStatus.FORBIDDEN, "Nice try");
        }
        return FunctionResult.PROCESSED;
    }

    private boolean isSubDirectory(File base, File child)
    {
        try
        {
            base = base.getCanonicalFile();
            child = child.getCanonicalFile();
        }
        catch (IOException exc)
        {
            throw new RuntimeException(exc);
        }
        File parentFile = child;
        while (parentFile != null)
        {
            if (base.equals(parentFile))
            {
                return true;
            }
            parentFile = parentFile.getParentFile();
        }
        return false;
    }
}