/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.processor.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

import infra.context.properties.processor.ConfigurationMetadataAnnotationProcessor;

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
        TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.MOCK_ENDPOINT_ANNOTATION,
        TestConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION,
        "infra.context.annotation.Configuration" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestConfigurationMetadataAnnotationProcessor extends ConfigurationMetadataAnnotationProcessor {

  public static final String CONFIGURATION_PROPERTIES_ANNOTATION = "infra.context.properties.sample.TestConfigurationProperties";

  public static final String CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION = "infra.context.properties.sample.TestConfigurationPropertiesSource";

  public static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "infra.context.properties.sample.TestNestedConfigurationProperty";

  public static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "infra.context.properties.sample.TestDeprecatedConfigurationProperty";

  public static final String CONSTRUCTOR_BINDING_ANNOTATION = "infra.context.properties.sample.TestConstructorBinding";

  public static final String AUTOWIRED_ANNOTATION = "infra.context.properties.sample.TestAutowired";

  public static final String DEFAULT_VALUE_ANNOTATION = "infra.context.properties.sample.TestDefaultValue";

  public static final String CONTROLLER_ENDPOINT_ANNOTATION = "infra.context.properties.sample.TestControllerEndpoint";

  public static final String ENDPOINT_ANNOTATION = "infra.context.properties.sample.TestEndpoint";

  public static final String JMX_ENDPOINT_ANNOTATION = "infra.context.properties.sample.TestJmxEndpoint";

  public static final String REST_CONTROLLER_ENDPOINT_ANNOTATION = "infra.context.properties.sample.TestRestControllerEndpoint";

  public static final String MOCK_ENDPOINT_ANNOTATION = "infra.context.properties.sample.TestMockEndpoint";

  public static final String WEB_ENDPOINT_ANNOTATION = "infra.context.properties.sample.TestWebEndpoint";

  public static final String READ_OPERATION_ANNOTATION = "infra.context.properties.sample.TestReadOperation";

  public static final String NAME_ANNOTATION = "infra.context.properties.sample.TestName";

  public TestConfigurationMetadataAnnotationProcessor() {
  }

  @Override
  protected String configurationPropertiesAnnotation() {
    return CONFIGURATION_PROPERTIES_ANNOTATION;
  }

  @Override
  protected String configurationPropertiesSourceAnnotation() {
    return CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION;
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
