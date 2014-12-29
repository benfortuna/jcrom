/**
 * This file is part of the JCROM project.
 * Copyright (C) 2008-2014 - All rights reserved.
 * Authors: Olafur Gauti Gudmundsson, Nicolas Dos Santos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jcrom.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jcrom.converter.Converter;
import org.jcrom.converter.DefaultConverter;

/**
 * This annotation is used mark fields that should be mapped to JCR node properties.
 * 
 * @author Olafur Gauti Gudmundsson
 * @author Nicolas Dos Santos
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
public @interface JcrProperty {

    /**
     * The name of the JCR property.
     * Defaults to the name of the field being annotated.
     * 
     * @return the name of the JCR property storing the value that the field represents
     */
    String name() default "fieldName";

    /**
     * Specifies the class implementing {@link Converter} used to convert an entity attribute value to JCR property representation and back again.
     * The default is {@link DefaultConverter} to indicate that there is no custom conversion.
     * 
     * @return the class implementing {@link Converter} to convert an entity attribute value to JCR property representation
     * @see Converter
     */
    Class<? extends Converter<?, ?>> converter() default DefaultConverter.class;

}
