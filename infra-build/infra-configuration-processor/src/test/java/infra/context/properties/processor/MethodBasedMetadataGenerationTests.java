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

package infra.context.properties.processor;

import org.junit.jupiter.api.Test;

import infra.context.properties.processor.metadata.ConfigurationMetadata;
import infra.context.properties.processor.metadata.Metadata;
import infra.context.properties.sample.method.DeprecatedClassMethodConfig;
import infra.context.properties.sample.method.DeprecatedMethodConfig;
import infra.context.properties.sample.method.EmptyTypeMethodConfig;
import infra.context.properties.sample.method.InvalidMethodConfig;
import infra.context.properties.sample.method.MethodAndClassConfig;
import infra.context.properties.sample.method.PackagePrivateMethodConfig;
import infra.context.properties.sample.method.PrivateMethodConfig;
import infra.context.properties.sample.method.ProtectedMethodConfig;
import infra.context.properties.sample.method.PublicMethodConfig;
import infra.context.properties.sample.method.SingleConstructorMethodConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for types defined by {@code @Bean} methods.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class MethodBasedMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void publicMethodConfig() {
    methodConfig(PublicMethodConfig.class, PublicMethodConfig.Foo.class);
  }

  @Test
  void protectedMethodConfig() {
    methodConfig(ProtectedMethodConfig.class, ProtectedMethodConfig.Foo.class);
  }

  @Test
  void packagePrivateMethodConfig() {
    methodConfig(PackagePrivateMethodConfig.class, PackagePrivateMethodConfig.Foo.class);
  }

  private void methodConfig(Class<?> config, Class<?> properties) {
    ConfigurationMetadata metadata = compile(config);
    assertThat(metadata).has(Metadata.withGroup("foo").fromSource(config));
    assertThat(metadata).has(Metadata.withProperty("foo.name", String.class).fromSource(properties));
    assertThat(metadata)
            .has(Metadata.withProperty("foo.flag", Boolean.class).withDefaultValue(false).fromSource(properties));
  }

  @Test
  void privateMethodConfig() {
    ConfigurationMetadata metadata = compile(PrivateMethodConfig.class);
    assertThat(metadata).isNull();
  }

  @Test
  void invalidMethodConfig() {
    ConfigurationMetadata metadata = compile(InvalidMethodConfig.class);
    assertThat(metadata)
            .has(Metadata.withProperty("something.name", String.class).fromSource(InvalidMethodConfig.class));
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("invalid.name"));
  }

  @Test
  void methodAndClassConfig() {
    ConfigurationMetadata metadata = compile(MethodAndClassConfig.class);
    assertThat(metadata)
            .has(Metadata.withProperty("conflict.name", String.class).fromSource(MethodAndClassConfig.Foo.class));
    assertThat(metadata).has(Metadata.withProperty("conflict.flag", Boolean.class)
            .withDefaultValue(false)
            .fromSource(MethodAndClassConfig.Foo.class));
    assertThat(metadata)
            .has(Metadata.withProperty("conflict.value", String.class).fromSource(MethodAndClassConfig.class));
  }

  @Test
  void singleConstructorMethodConfig() {
    ConfigurationMetadata metadata = compile(SingleConstructorMethodConfig.class);
    assertThat(metadata).doesNotHave(Metadata.withProperty("foo.my-service", Object.class)
            .fromSource(SingleConstructorMethodConfig.Foo.class));
    assertThat(metadata)
            .has(Metadata.withProperty("foo.name", String.class).fromSource(SingleConstructorMethodConfig.Foo.class));
    assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
            .withDefaultValue(false)
            .fromSource(SingleConstructorMethodConfig.Foo.class));
  }

  @Test
  void emptyTypeMethodConfig() {
    ConfigurationMetadata metadata = compile(EmptyTypeMethodConfig.class);
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("something.foo"));
  }

  @Test
  void deprecatedMethodConfig() {
    Class<DeprecatedMethodConfig> type = DeprecatedMethodConfig.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
            .fromSource(DeprecatedMethodConfig.Foo.class)
            .withDeprecation());
    assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
            .withDefaultValue(false)
            .fromSource(DeprecatedMethodConfig.Foo.class)
            .withDeprecation());
  }

  @Test
  @SuppressWarnings("deprecation")
  void deprecatedMethodConfigOnClass() {
    Class<?> type = DeprecatedClassMethodConfig.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
            .fromSource(DeprecatedClassMethodConfig.Foo.class)
            .withDeprecation());
    assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
            .withDefaultValue(false)
            .fromSource(DeprecatedClassMethodConfig.Foo.class)
            .withDeprecation());
  }

}
