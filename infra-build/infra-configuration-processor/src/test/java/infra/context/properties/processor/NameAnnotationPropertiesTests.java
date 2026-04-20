/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.context.properties.processor;

import org.junit.jupiter.api.Test;

import infra.context.properties.processor.metadata.ConfigurationMetadata;
import infra.context.properties.processor.metadata.Metadata;
import infra.context.properties.sample.name.ConstructorParameterNameAnnotationProperties;
import infra.context.properties.sample.name.DuplicateNameConstructorParameterProperties;
import infra.context.properties.sample.name.DuplicateNameJavaBeanProperties;
import infra.context.properties.sample.name.JavaBeanNameAnnotationProperties;
import infra.context.properties.sample.name.LombokNameAnnotationProperties;
import infra.context.properties.sample.name.RecordComponentNameAnnotationProperties;
import infra.core.test.tools.CompilationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Metadata generation tests for immutable properties using {@code @Name}.
 *
 * @author Phillip Webb
 */
class NameAnnotationPropertiesTests extends AbstractMetadataGenerationTests {

  @Test
  void constructorParameterNameAnnotationProperties() {
    ConfigurationMetadata metadata = compile(ConstructorParameterNameAnnotationProperties.class);
    assertThat(metadata)
            .has(Metadata.withProperty("named.import", String.class)
                    .fromSource(ConstructorParameterNameAnnotationProperties.class)
                    .withDescription("Imports to apply."))
            .has(Metadata.withProperty("named.default", Boolean.class)
                    .fromSource(ConstructorParameterNameAnnotationProperties.class)
                    .withDefaultValue("Whether default mode is enabled.")
                    .withDefaultValue(true));
  }

  @Test
  void recordComponentNameAnnotationProperties() {
    ConfigurationMetadata metadata = compile(RecordComponentNameAnnotationProperties.class);
    assertThat(metadata)
            .has(Metadata.withProperty("named.import", String.class)
                    .fromSource(RecordComponentNameAnnotationProperties.class)
                    .withDescription("Imports to apply."))
            .has(Metadata.withProperty("named.default", Boolean.class)
                    .fromSource(RecordComponentNameAnnotationProperties.class)
                    .withDefaultValue("Whether default mode is enabled.")
                    .withDefaultValue(true));
  }

  @Test
  void javaBeanNameAnnotationProperties() {
    ConfigurationMetadata metadata = compile(JavaBeanNameAnnotationProperties.class);
    assertThat(metadata)
            .has(Metadata.withProperty("named.import", String.class)
                    .fromSource(JavaBeanNameAnnotationProperties.class)
                    .withDescription("Imports to apply."))
            .has(Metadata.withProperty("named.default", Boolean.class)
                    .fromSource(JavaBeanNameAnnotationProperties.class)
                    .withDefaultValue("Whether default mode is enabled.")
                    .withDefaultValue(true));
  }

  @Test
  void lombokNameAnnotationProperties() {
    ConfigurationMetadata metadata = compile(LombokNameAnnotationProperties.class);
    assertThat(metadata)
            .has(Metadata.withProperty("named.import", String.class)
                    .fromSource(LombokNameAnnotationProperties.class)
                    .withDescription("Imports to apply."))
            .has(Metadata.withProperty("named.default", Boolean.class)
                    .fromSource(LombokNameAnnotationProperties.class)
                    .withDefaultValue("Whether default mode is enabled.")
                    .withDefaultValue(true));
  }

  @Test
  void duplicateNameOnJavaBeanPropertiesFailsCompilation() {
    assertThatExceptionOfType(CompilationException.class)
            .isThrownBy(() -> compile(DuplicateNameJavaBeanProperties.class))
            .withMessageContaining("Unable to compile source")
            .withMessageContaining("Property name 'aaa' maps to distinct properties in type ")
            .withMessageContaining(DuplicateNameJavaBeanProperties.class.getName());
  }

  @Test
  void duplicateNameOnConstructorParametersFailsCompilation() {
    assertThatExceptionOfType(CompilationException.class)
            .isThrownBy(() -> compile(DuplicateNameConstructorParameterProperties.class))
            .withMessageContaining("Unable to compile source")
            .withMessageContaining("Property name 'aaa' maps to distinct properties in type ")
            .withMessageContaining(DuplicateNameConstructorParameterProperties.class.getName());
  }

}
