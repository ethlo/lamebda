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
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ethlo.lamebda.ProjectImpl;
import com.ethlo.lamebda.loader.ConfigLoader;
import com.ethlo.lamebda.util.HttpUtil;
import com.ethlo.lamebda.util.IoUtil;

public class HttpCloudConfigLoader implements ConfigLoader
{
    public static final String DEPLOYMENT_ARTIFACT_URL = "deployment.artifact-url";
    private static final Logger logger = LoggerFactory.getLogger(HttpCloudConfigLoader.class);
    private final Path rootDirectory;
    private final URL configServerUrl;
    private final String applicationName;
    private final String profileName;
    private final String label;
    private final Set<String> projectNames;

    public HttpCloudConfigLoader(final Path rootDirectory, final URL configServerUrl, final String applicationName, final String profileName, final String label, final Set<String> projectNames)
    {
        this.rootDirectory = rootDirectory;
        this.configServerUrl = configServerUrl;
        this.applicationName = applicationName;
        this.profileName = profileName;
        this.label = label;
        this.projectNames = projectNames;
    }

    @Override
    public void prepareConfig()
    {
        for (String projectName : projectNames)
        {
            final String fullUrl = configServerUrl.toExternalForm() + "/" + applicationName + "/" + profileName + "/" + label + "/lamebda-" + projectName + ".properties";
            logger.info("Cloud config URL for project {}: {}", projectName, fullUrl);
            try
            {
                final String configContent = IoUtil.toString(HttpUtil.getContent(fullUrl).getInputStream(), StandardCharsets.UTF_8);
                logger.debug("Properties content: {}", configContent);
                final Path projectPath = rootDirectory.toAbsolutePath().resolve(projectName);
                Files.createDirectories(projectPath);
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
        final Path localConfigPath = projectPath.resolve(ProjectImpl.DEFAULT_CONFIG_FILENAME);
        Files.write(localConfigPath, configContent.getBytes(StandardCharsets.UTF_8));
        logger.info("Installed configuration from {} into {}", fullUrl, localConfigPath);
    }
}
