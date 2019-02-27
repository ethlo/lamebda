package com.ethlo.lamebda.util;

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

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstraintValidator
{
    private static final Logger logger = LoggerFactory.getLogger(ConstraintValidator.class);
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    public static void assertValid(Object object)
    {
        final Validator validator = factory.getValidator();
        Set<ConstraintViolation<Object>> errors = validator.validate(object);
        if (!errors.isEmpty())
        {
            errors.forEach(error -> logger.info("{} validation failed. Message={}, Path={}, Value=[{}]", object.getClass().getSimpleName(), error.getMessage(), error.getPropertyPath(), error.getInvalidValue()));
            throw new ConstraintViolationException(errors);
        }
    }
}
