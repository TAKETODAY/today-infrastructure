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
import cn.taketoday.context.properties.sample.immutable.ImmutableNameAnnotationProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for immutable properties using {@code @Name}.
 *
 * @author Phillip Webb
 */
class ImmutableNameAnnotationPropertiesTests extends AbstractMetadataGenerationTests {

  @Test
  void immutableNameAnnotationProperties() {
    ConfigurationMetadata metadata = compile(ImmutableNameAnnotationProperties.class);
    assertThat(metadata).has(Metadata.withProperty("named.import", String.class)
            .fromSource(ImmutableNameAnnotationProperties.class));
  }

}
