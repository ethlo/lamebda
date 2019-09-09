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

import static com.ethlo.lamebda.loader.http.HttpCloudConfigLoader.DEPLOYMENT_ARTIFACT_URL;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import com.ethlo.lamebda.ProjectImpl;
import com.ethlo.lamebda.loader.ArtifactLoader;
import com.ethlo.lamebda.util.HttpUtil;

public class HttpArtifactLoader implements ArtifactLoader
{
    private static final Logger logger = LoggerFactory.getLogger(HttpArtifactLoader.class);

    @Override
    public void prepareArtifact(Path projectPath) throws IOException
    {
        final String projectName = projectPath.getFileName().toString();

        final Path configFile = projectPath.resolve(ProjectImpl.PROJECT_FILENAME);
        if (Files.exists(configFile))
        {
            final Properties properties = new Properties();
            properties.load(Files.newBufferedReader(configFile));
            installArtifactFromUrl(projectName, projectPath, properties);
        }
    }

    private void installArtifactFromUrl(final String projectName, final Path projectPath, final Properties properties) throws IOException
    {
        final String artifactUrl = properties.getProperty(DEPLOYMENT_ARTIFACT_URL);
        if (artifactUrl != null)
        {
            logger.info("Found artifact URL for project {}: {}", projectName, artifactUrl);
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
            logger.debug("No '" + DEPLOYMENT_ARTIFACT_URL + "' property defined in config file. Skipping new deployment.");
        }
    }

    private Resource getArtifact(final String artifactUrl)
    {
        return HttpUtil.getContent(artifactUrl);
    }
}
