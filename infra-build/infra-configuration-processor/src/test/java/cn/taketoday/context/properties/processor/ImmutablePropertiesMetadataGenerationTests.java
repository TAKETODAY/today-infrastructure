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
import cn.taketoday.context.properties.sample.immutable.ImmutableSimpleProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for immutable properties.
 *
 * @author Stephane Nicoll
 */
class ImmutablePropertiesMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void immutableSimpleProperties() {
    ConfigurationMetadata metadata = compile(ImmutableSimpleProperties.class);
    assertThat(metadata).has(Metadata.withGroup("immutable").fromSource(ImmutableSimpleProperties.class));
    assertThat(metadata).has(Metadata.withProperty("immutable.the-name", String.class)
            .fromSource(ImmutableSimpleProperties.class)
            .withDescription("The name of this simple properties.")
            .withDefaultValue("boot"));
    assertThat(metadata).has(Metadata.withProperty("immutable.flag", Boolean.class)
            .withDefaultValue(false)
            .fromSource(ImmutableSimpleProperties.class)
            .withDescription("A simple flag.")
            .withDeprecation());
    assertThat(metadata).has(Metadata.withProperty("immutable.comparator"));
    assertThat(metadata).has(Metadata.withProperty("immutable.counter"));
    assertThat(metadata.getItems()).hasSize(5);
  }

}
