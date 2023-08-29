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
import cn.taketoday.context.properties.sample.generic.AbstractGenericProperties;
import cn.taketoday.context.properties.sample.generic.ComplexGenericProperties;
import cn.taketoday.context.properties.sample.generic.ConcreteBuilderProperties;
import cn.taketoday.context.properties.sample.generic.GenericConfig;
import cn.taketoday.context.properties.sample.generic.SimpleGenericProperties;
import cn.taketoday.context.properties.sample.generic.UnresolvedGenericProperties;
import cn.taketoday.context.properties.sample.generic.UpperBoundGenericPojo;
import cn.taketoday.context.properties.sample.generic.WildcardConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for generics handling.
 *
 * @author Stephane Nicoll
 */
class GenericsMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void simpleGenericProperties() {
    ConfigurationMetadata metadata = compile(AbstractGenericProperties.class, SimpleGenericProperties.class);
    assertThat(metadata).has(Metadata.withGroup("generic").fromSource(SimpleGenericProperties.class));
    assertThat(metadata).has(Metadata.withProperty("generic.name", String.class)
            .fromSource(SimpleGenericProperties.class)
            .withDescription("Generic name.")
            .withDefaultValue(null));
    assertThat(metadata)
            .has(Metadata.withProperty("generic.mappings", "java.util.Map<java.lang.Integer,java.time.Duration>")
                    .fromSource(SimpleGenericProperties.class)
                    .withDescription("Generic mappings.")
                    .withDefaultValue(null));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void complexGenericProperties() {
    ConfigurationMetadata metadata = compile(ComplexGenericProperties.class);
    assertThat(metadata).has(Metadata.withGroup("generic").fromSource(ComplexGenericProperties.class));
    assertThat(metadata).has(Metadata.withGroup("generic.test")
            .ofType(UpperBoundGenericPojo.class)
            .fromSource(ComplexGenericProperties.class));
    assertThat(metadata)
            .has(Metadata.withProperty("generic.test.mappings", "java.util.Map<java.lang.Enum<T>,java.lang.String>")
                    .fromSource(UpperBoundGenericPojo.class));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void unresolvedGenericProperties() {
    ConfigurationMetadata metadata = compile(AbstractGenericProperties.class, UnresolvedGenericProperties.class);
    assertThat(metadata).has(Metadata.withGroup("generic").fromSource(UnresolvedGenericProperties.class));
    assertThat(metadata).has(Metadata.withProperty("generic.name", String.class)
            .fromSource(UnresolvedGenericProperties.class)
            .withDescription("Generic name.")
            .withDefaultValue(null));
    assertThat(metadata)
            .has(Metadata.withProperty("generic.mappings", "java.util.Map<java.lang.Number,java.lang.Object>")
                    .fromSource(UnresolvedGenericProperties.class)
                    .withDescription("Generic mappings.")
                    .withDefaultValue(null));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void genericTypes() {
    ConfigurationMetadata metadata = compile(GenericConfig.class);
    assertThat(metadata).has(Metadata.withGroup("generic")
            .ofType("cn.taketoday.context.properties.sample.generic.GenericConfig"));
    assertThat(metadata).has(Metadata.withGroup("generic.foo")
            .ofType("cn.taketoday.context.properties.sample.generic.GenericConfig$Foo"));
    assertThat(metadata).has(Metadata.withGroup("generic.foo.bar")
            .ofType("cn.taketoday.context.properties.sample.generic.GenericConfig$Bar"));
    assertThat(metadata).has(Metadata.withGroup("generic.foo.bar.biz")
            .ofType("cn.taketoday.context.properties.sample.generic.GenericConfig$Bar$Biz"));
    assertThat(metadata)
            .has(Metadata.withProperty("generic.foo.name").ofType(String.class).fromSource(GenericConfig.Foo.class));
    assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-bar")
            .ofType("java.util.Map<java.lang.String,cn.taketoday.context.properties.sample.generic.GenericConfig$Bar<java.lang.Integer>>")
            .fromSource(GenericConfig.Foo.class));
    assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-integer")
            .ofType("java.util.Map<java.lang.String,java.lang.Integer>")
            .fromSource(GenericConfig.Foo.class));
    assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.name")
            .ofType("java.lang.String")
            .fromSource(GenericConfig.Bar.class));
    assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.biz.name")
            .ofType("java.lang.String")
            .fromSource(GenericConfig.Bar.Biz.class));
    assertThat(metadata.getItems()).hasSize(9);
  }

  @Test
  void wildcardTypes() {
    ConfigurationMetadata metadata = compile(WildcardConfig.class);
    assertThat(metadata).has(Metadata.withGroup("wildcard").ofType(WildcardConfig.class));
    assertThat(metadata).has(Metadata.withProperty("wildcard.string-to-number")
            .ofType("java.util.Map<java.lang.String,? extends java.lang.Number>")
            .fromSource(WildcardConfig.class));
    assertThat(metadata).has(Metadata.withProperty("wildcard.integers")
            .ofType("java.util.List<? super java.lang.Integer>")
            .fromSource(WildcardConfig.class));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void builderPatternWithGenericReturnType() {
    ConfigurationMetadata metadata = compile(ConcreteBuilderProperties.class);
    assertThat(metadata).has(Metadata.withGroup("builder").fromSource(ConcreteBuilderProperties.class));
    assertThat(metadata)
            .has(Metadata.withProperty("builder.number", Integer.class).fromSource(ConcreteBuilderProperties.class));
    assertThat(metadata).has(
            Metadata.withProperty("builder.description", String.class).fromSource(ConcreteBuilderProperties.class));
    assertThat(metadata.getItems()).hasSize(3);
  }

}
