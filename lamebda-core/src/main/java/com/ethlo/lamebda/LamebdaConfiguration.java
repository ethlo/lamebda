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

public class LamebdaConfiguration
{
    private boolean validateSpecification;
    private boolean generateApiDoc;
    private boolean generateModels;
    private boolean generateHandlers;

    public boolean isValidateSpecification()
    {
        return validateSpecification;
    }

    public boolean isGenerateApiDoc()
    {
        return generateApiDoc;
    }

    public boolean isGenerateModels()
    {
        return generateModels;
    }

    public boolean isGenerateHandlers()
    {
        return generateHandlers;
    }

    public static final class LamebdaConfigurationBuilder
    {
        private boolean validateSpecification = true;
        private boolean generateApiDoc = true;
        private boolean generateModels = true;
        private boolean generateHandlers = false;

        private LamebdaConfigurationBuilder()
        {
        }

        public static LamebdaConfigurationBuilder aLamebdaConfiguration()
        {
            return new LamebdaConfigurationBuilder();
        }

        public LamebdaConfigurationBuilder validateSpecification(boolean validateSpecification)
        {
            this.validateSpecification = validateSpecification;
            return this;
        }

        public LamebdaConfigurationBuilder generateApiDoc(boolean generateApiDoc)
        {
            this.generateApiDoc = generateApiDoc;
            return this;
        }

        public LamebdaConfigurationBuilder generateModels(boolean generateModels)
        {
            this.generateModels = generateModels;
            return this;
        }

        public LamebdaConfigurationBuilder generateHandlers(boolean generateHandlers)
        {
            this.generateHandlers = generateHandlers;
            return this;
        }

        public LamebdaConfiguration build()
        {
            LamebdaConfiguration lamebdaConfiguration = new LamebdaConfiguration();
            lamebdaConfiguration.validateSpecification = this.validateSpecification;
            lamebdaConfiguration.generateHandlers = this.generateHandlers;
            lamebdaConfiguration.generateApiDoc = this.generateApiDoc;
            lamebdaConfiguration.generateModels = this.generateModels;
            return lamebdaConfiguration;
        }
    }
}
