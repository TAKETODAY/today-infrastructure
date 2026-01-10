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
import infra.context.properties.sample.inheritance.ChildProperties;
import infra.context.properties.sample.inheritance.ChildPropertiesConfig;
import infra.context.properties.sample.inheritance.OverrideChildProperties;
import infra.context.properties.sample.inheritance.OverrideChildPropertiesConfig;

import static org.assertj.core.api.Assertions.assertThat;

class InheritanceMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void childProperties() {
    ConfigurationMetadata metadata = compile(ChildPropertiesConfig.class);
    assertThat(metadata).has(Metadata.withGroup("inheritance").fromSource(ChildPropertiesConfig.class));
    assertThat(metadata).has(Metadata.withGroup("inheritance.nest").fromSource(ChildProperties.class));
    assertThat(metadata).has(Metadata.withGroup("inheritance.child-nest").fromSource(ChildProperties.class));
    assertThat(metadata).has(Metadata.withProperty("inheritance.bool-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.int-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.long-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.nest.bool-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.nest.int-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.child-nest.bool-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.child-nest.int-value"));
  }

  @Test
  void overrideChildProperties() {
    ConfigurationMetadata metadata = compile(OverrideChildPropertiesConfig.class);
    assertThat(metadata).has(Metadata.withGroup("inheritance").fromSource(OverrideChildPropertiesConfig.class));
    assertThat(metadata).has(Metadata.withGroup("inheritance.nest").fromSource(OverrideChildProperties.class));
    assertThat(metadata).has(Metadata.withProperty("inheritance.bool-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.int-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.long-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.nest.bool-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.nest.int-value"));
    assertThat(metadata).has(Metadata.withProperty("inheritance.nest.long-value"));

  }

}
