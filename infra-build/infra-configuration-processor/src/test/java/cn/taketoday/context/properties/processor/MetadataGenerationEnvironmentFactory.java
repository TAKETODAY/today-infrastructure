/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.processor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.processing.ProcessingEnvironment;

import cn.taketoday.context.properties.processor.test.TestConfigurationMetadataAnnotationProcessor;

/**
 * A factory for {@link MetadataGenerationEnvironment} against test annotations.
 *
 * @author Stephane Nicoll
 */
class MetadataGenerationEnvironmentFactory implements Function<ProcessingEnvironment, MetadataGenerationEnvironment> {

  @Override
  public MetadataGenerationEnvironment apply(ProcessingEnvironment environment) {
    Set<String> endpointAnnotations = new HashSet<>(
            Arrays.asList(TestConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
                    TestConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
                    TestConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
                    TestConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
                    TestConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION));
    return new MetadataGenerationEnvironment(environment,
            TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
            TestConfigurationMetadataAnnotationProcessor.NESTED_CONFIGURATION_PROPERTY_ANNOTATION,
            TestConfigurationMetadataAnnotationProcessor.DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION,
            TestConfigurationMetadataAnnotationProcessor.CONSTRUCTOR_BINDING_ANNOTATION,
            TestConfigurationMetadataAnnotationProcessor.AUTOWIRED_ANNOTATION,
            TestConfigurationMetadataAnnotationProcessor.DEFAULT_VALUE_ANNOTATION, endpointAnnotations,
            TestConfigurationMetadataAnnotationProcessor.READ_OPERATION_ANNOTATION,
            TestConfigurationMetadataAnnotationProcessor.NAME_ANNOTATION);
  }

}
