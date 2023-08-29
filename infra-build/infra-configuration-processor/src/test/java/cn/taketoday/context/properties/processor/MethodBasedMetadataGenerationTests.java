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

import org.junit.jupiter.api.Test;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.Metadata;
import cn.taketoday.context.properties.sample.method.DeprecatedClassMethodConfig;
import cn.taketoday.context.properties.sample.method.DeprecatedMethodConfig;
import cn.taketoday.context.properties.sample.method.EmptyTypeMethodConfig;
import cn.taketoday.context.properties.sample.method.InvalidMethodConfig;
import cn.taketoday.context.properties.sample.method.MethodAndClassConfig;
import cn.taketoday.context.properties.sample.method.PackagePrivateMethodConfig;
import cn.taketoday.context.properties.sample.method.PrivateMethodConfig;
import cn.taketoday.context.properties.sample.method.ProtectedMethodConfig;
import cn.taketoday.context.properties.sample.method.PublicMethodConfig;
import cn.taketoday.context.properties.sample.method.SingleConstructorMethodConfig;

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
