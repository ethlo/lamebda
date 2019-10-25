package com.ethlo.lamebda;

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

import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.util.StringUtils;

import com.ethlo.lamebda.util.ConstraintValidator;

public class ConfigurationUtil
{
    public static void populate(final BootstrapConfiguration cfg, Properties properties)
    {
        if (properties.isEmpty())
        {
            return;
        }

        final UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
        final BindHandler handler = new NoUnboundElementsBindHandler(new IgnoreTopLevelConverterNotFoundBindHandler(), filter);
        final Binder binder = new Binder(new MapConfigurationPropertySource(properties));
        final Bindable<BootstrapConfiguration> target = Bindable.ofInstance(cfg);
        try
        {
            final BootstrapConfiguration projectConfiguration = binder.bind("", target, handler).get();
            ConstraintValidator.assertValid(projectConfiguration);
        }
        catch (BindException exc)
        {
            if (exc.getCause() instanceof UnboundConfigurationPropertiesException)
            {
                final UnboundConfigurationPropertiesException e = (UnboundConfigurationPropertiesException) exc.getCause();
                throw new IllegalArgumentException("Unbound project.properties entries: " + StringUtils.collectionToCommaDelimitedString(e.getUnboundProperties().stream().map(p -> p.getName() + "=" + p.getValue()).collect(Collectors.toList())));
            }
        }
    }
}
