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
import cn.taketoday.context.properties.sample.inheritance.ChildProperties;
import cn.taketoday.context.properties.sample.inheritance.ChildPropertiesConfig;
import cn.taketoday.context.properties.sample.inheritance.OverrideChildProperties;
import cn.taketoday.context.properties.sample.inheritance.OverrideChildPropertiesConfig;

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
