package com.ethlo.lamebda.loader.http;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
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

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import com.ethlo.lamebda.util.IoUtil;

public class HttpRepositoryProjectLoaderTest
{
    private static ClientAndServer mockServer;

    @BeforeClass
    public static void startServer()
    {
        mockServer = startClientAndServer(1080);
        createResponseForCloudConfigRequest();
        createResponseForArtifactDownload();
    }

    @AfterClass
    public static void stopServer()
    {
        mockServer.stop();
    }

    private static void createResponseForCloudConfigRequest()
    {
        new MockServerClient("127.0.0.1", 1080)
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/app/default/master/lamebda-foobar.properties"),
                        exactly(1)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "text/plain; charset=utf-8"))
                                .withBody(IoUtil.toString("/config/lamebda-foobar.properties").get())
                );
    }

    private static void createResponseForArtifactDownload()
    {
        new MockServerClient("127.0.0.1", 1080)
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/repo/foobar.jar"),
                        exactly(1)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/jar"))
                                .withBody(IoUtil.toString("/config/lamebda-foobar.properties").get())
                );
    }

    @Test
    public void prepare() throws IOException
    {
        final Path rootDirectory = Files.createTempDirectory("lamebda-junit");
        final URL configServerUrl = new URL("http://localhost:1080");
        final String applicationName = "app";
        final String profileName = "default";
        final String labelName = "master";
        final HttpCloudConfigLoader l = new HttpCloudConfigLoader(rootDirectory, configServerUrl, applicationName, profileName, labelName, Collections.singleton("foobar"));
        l.prepareConfig();
    }
}
