/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.processor.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

import cn.taketoday.context.properties.processor.ConfigurationMetadataAnnotationProcessor;

/**
 * Test {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @author Scott Frederick
 */
@SupportedAnnotationTypes({ TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.MOCK_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION,
        "cn.taketoday.context.annotation.Configuration" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestConfigurationMetadataAnnotationProcessor extends ConfigurationMetadataAnnotationProcessor {

  public static final String CONFIGURATION_PROPERTIES_ANNOTATION = "cn.taketoday.context.properties.sample.ConfigurationProperties";

  public static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "cn.taketoday.context.properties.sample.NestedConfigurationProperty";

  public static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "cn.taketoday.context.properties.sample.DeprecatedConfigurationProperty";

  public static final String CONSTRUCTOR_BINDING_ANNOTATION = "cn.taketoday.context.properties.sample.ConstructorBinding";

  public static final String AUTOWIRED_ANNOTATION = "cn.taketoday.context.properties.sample.Autowired";

  public static final String DEFAULT_VALUE_ANNOTATION = "cn.taketoday.context.properties.sample.DefaultValue";

  public static final String CONTROLLER_ENDPOINT_ANNOTATION = "cn.taketoday.context.properties.sample.ControllerEndpoint";

  public static final String ENDPOINT_ANNOTATION = "cn.taketoday.context.properties.sample.Endpoint";

  public static final String JMX_ENDPOINT_ANNOTATION = "cn.taketoday.context.properties.sample.JmxEndpoint";

  public static final String REST_CONTROLLER_ENDPOINT_ANNOTATION = "cn.taketoday.context.properties.sample.RestControllerEndpoint";

  public static final String MOCK_ENDPOINT_ANNOTATION = "cn.taketoday.context.properties.sample.MockEndpoint";

  public static final String WEB_ENDPOINT_ANNOTATION = "cn.taketoday.context.properties.sample.WebEndpoint";

  public static final String READ_OPERATION_ANNOTATION = "cn.taketoday.context.properties.sample.ReadOperation";

  public static final String NAME_ANNOTATION = "cn.taketoday.context.properties.sample.Name";

  public TestConfigurationMetadataAnnotationProcessor() {
  }

  @Override
  protected String configurationPropertiesAnnotation() {
    return CONFIGURATION_PROPERTIES_ANNOTATION;
  }

  @Override
  protected String nestedConfigurationPropertyAnnotation() {
    return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
  }

  @Override
  protected String deprecatedConfigurationPropertyAnnotation() {
    return DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION;
  }

  @Override
  protected String constructorBindingAnnotation() {
    return CONSTRUCTOR_BINDING_ANNOTATION;
  }

  @Override
  protected String autowiredAnnotation() {
    return AUTOWIRED_ANNOTATION;
  }

  @Override
  protected String defaultValueAnnotation() {
    return DEFAULT_VALUE_ANNOTATION;
  }

  @Override
  protected Set<String> endpointAnnotations() {
    return new HashSet<>(Arrays.asList(CONTROLLER_ENDPOINT_ANNOTATION, ENDPOINT_ANNOTATION, JMX_ENDPOINT_ANNOTATION,
            REST_CONTROLLER_ENDPOINT_ANNOTATION, MOCK_ENDPOINT_ANNOTATION, WEB_ENDPOINT_ANNOTATION));
  }

  @Override
  protected String readOperationAnnotation() {
    return READ_OPERATION_ANNOTATION;
  }

  @Override
  protected String nameAnnotation() {
    return NAME_ANNOTATION;
  }

}
