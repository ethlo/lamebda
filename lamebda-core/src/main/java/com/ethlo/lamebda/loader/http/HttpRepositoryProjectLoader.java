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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public static final String DEPLOYMENT_ARTIFACT_URL = "deployment.artifact-url";
    private final Path rootDirectory;
    private final URL configServerUrl;
    private final String applicationName;
    private final String profileName;
    private final String label;
    private final Set<String> projectNames;

    public HttpRepositoryProjectLoader(final Path rootDirectory, final URL configServerUrl, final String applicationName, final String profileName, final String label, final Set<String> projectNames)
    {
        this.rootDirectory = rootDirectory;
        this.configServerUrl = configServerUrl;
        this.applicationName = applicationName;
        this.profileName = profileName;
        this.label = label;
        this.projectNames = projectNames;
    }

    @Override
    public void prepare()
    {
        for (String projectName : projectNames)
        {
            final String fullUrl = configServerUrl.toExternalForm() + "/" + applicationName + "/" + profileName + "/" + label + "/lamebda-" + projectName + ".properties";
            logger.info("Cloud config URL for project {}: {}", projectName, fullUrl);
            try
            {
                final Properties properties = new Properties();
                final String configContent = IoUtil.toString(HttpUtil.getContent(fullUrl).getInputStream(), StandardCharsets.UTF_8);
                logger.debug("Properties content: {}", configContent);
                properties.load(new StringReader(configContent));

                final Path projectPath = rootDirectory.toAbsolutePath().resolve(projectName);
                Files.createDirectories(projectPath);

                installArtifactFromUrl(projectName, projectPath, properties);
                installConfigFromUrl(fullUrl, configContent, projectPath);
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

    private void installArtifactFromUrl(final String projectName, final Path projectPath, final Properties properties) throws IOException
    {
        final String artifactUrl = properties.getProperty(DEPLOYMENT_ARTIFACT_URL);
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
        else
        {
            logger.info("No '" + DEPLOYMENT_ARTIFACT_URL + "' property defined in config file. Skipping new deployment.");
        }
    }

    private Resource getArtifact(final String artifactUrl)
    {
        return HttpUtil.getContent(artifactUrl);
    }
}
