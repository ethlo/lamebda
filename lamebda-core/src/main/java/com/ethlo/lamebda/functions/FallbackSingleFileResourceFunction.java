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
import java.util.Optional;

import com.ethlo.lamebda.HttpMimeType;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.util.FileNameUtil;
import com.ethlo.lamebda.util.IoUtil;

/**
 * Simple single resource function that will attempt to load the file first, then the classpath resource if not found
 */
public class FallbackSingleFileResourceFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private final Path filePath;
    private final String contentType;
    private final String classpathLocation;

    public FallbackSingleFileResourceFunction(String urlPath, Path filePath, String classpathLocation)
    {
        super(urlPath);

        if (filePath == null && classpathLocation == null)
        {
            throw new IllegalArgumentException("filePath and classpathLocation cannot both be null");
        }

        this.filePath = filePath;
        this.classpathLocation = classpathLocation;
        this.contentType = HttpMimeType.fromExtension(FileNameUtil.getExtension(filePath.getFileName().toString()));
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws IOException
    {
        response.setContentType(contentType);

        if (classpathLocation != null)
        {
            Optional<byte[]> data = IoUtil.classPathResource(classpathLocation);
            if (data.isPresent())
            {
                response.write(data.get());
                return;
            }
        }

        if (filePath != null)
        {
            if (Files.exists(filePath))
            {
                response.write(Files.readAllBytes(filePath));
                return;
            }
        }

        response.error(HttpStatus.NOT_FOUND, request.path() + " not found");
    }
}
