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
import infra.context.properties.sample.generic.AbstractGenericProperties;
import infra.context.properties.sample.generic.ComplexGenericProperties;
import infra.context.properties.sample.generic.ConcreteBuilderProperties;
import infra.context.properties.sample.generic.GenericConfig;
import infra.context.properties.sample.generic.SimpleGenericProperties;
import infra.context.properties.sample.generic.UnresolvedGenericProperties;
import infra.context.properties.sample.generic.UpperBoundGenericPojo;
import infra.context.properties.sample.generic.WildcardConfig;

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
            .ofType("infra.context.properties.sample.generic.GenericConfig"));
    assertThat(metadata).has(Metadata.withGroup("generic.foo")
            .ofType("infra.context.properties.sample.generic.GenericConfig$Foo"));
    assertThat(metadata).has(Metadata.withGroup("generic.foo.bar")
            .ofType("infra.context.properties.sample.generic.GenericConfig$Bar"));
    assertThat(metadata).has(Metadata.withGroup("generic.foo.bar.biz")
            .ofType("infra.context.properties.sample.generic.GenericConfig$Bar$Biz"));
    assertThat(metadata)
            .has(Metadata.withProperty("generic.foo.name").ofType(String.class).fromSource(GenericConfig.Foo.class));
    assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-bar")
            .ofType("java.util.Map<java.lang.String,infra.context.properties.sample.generic.GenericConfig$Bar<java.lang.Integer>>")
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
