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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import com.ethlo.lamebda.FunctionManagerImpl;
import com.ethlo.lamebda.loader.ProjectLoader;
import com.ethlo.lamebda.util.HttpUtil;
import com.ethlo.lamebda.util.IoUtil;

public class HttpRepositoryProjectLoader implements ProjectLoader
{
    private static final Logger logger = LoggerFactory.getLogger(HttpRepositoryProjectLoader.class);

    public HttpRepositoryProjectLoader(final Path rootDirectory, final URL configServerUrl, final String applicationName, final String profileName, final String label, final Set<String> projectNames)
    {
        for (String projectName : projectNames)
        {
            final String fullUrl = configServerUrl.toExternalForm() + "/" + applicationName + "/" + profileName + "/" + label + "/lamebda-" + projectName + ".properties";
            logger.info("Cloud config URL for project {}: {}", projectName, fullUrl);
            try
            {
                final Properties properties = new Properties();
                final String configContent = IoUtil.toString(HttpUtil.getContent(fullUrl).getInputStream(), StandardCharsets.UTF_8);
                properties.load(new StringReader(configContent));
                logger.info("Properties: {}", properties);

                final Path projectPath = rootDirectory.toAbsolutePath().resolve(projectName);

                installConfigFromUrl(fullUrl, configContent, projectPath);

                final String artifactUrl = properties.getProperty("deployment.artifact-url");
                installArtifactFromUrl(projectName, projectPath, artifactUrl);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void installConfigFromUrl(final String fullUrl, final String configContent, final Path projectPath) throws IOException
    {
        final Path localConfigPath = projectPath.resolve(FunctionManagerImpl.DEFAULT_CONFIG_FILENAME);
        Files.write(localConfigPath, configContent.getBytes(StandardCharsets.UTF_8));
        logger.info("Installed configuration from {} into {}", fullUrl, localConfigPath);
    }

    private void installArtifactFromUrl(final String projectName, final Path projectPath, final String artifactUrl) throws IOException
    {
        if (artifactUrl != null)
        {
            final Resource artifact = getArtifact(artifactUrl);
            logger.info("Downloaded artifact: " + artifact);

            final Path localArtifactPath = projectPath.resolve(projectName + ".jar");

            Files.createDirectories(localArtifactPath.getParent());
            try (final OutputStream out = Files.newOutputStream(localArtifactPath))
            {
                StreamUtils.copy(artifact.getInputStream(), out);
            }

            logger.info("Installed artifact from {} into {}", artifact.getDescription(), localArtifactPath);
        }
    }

    private Resource getArtifact(final String artifactUrl)
    {
        return HttpUtil.getContent(artifactUrl);
    }

    @Override
    public List<String> getProjectIds()
    {
        return null;
    }

    @Override
    public Path init()
    {
        return null;
    }

    private void loadProjectResources(URL url) throws IOException
    {
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        final int responseCode = con.getResponseCode();
    }
}
