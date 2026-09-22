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

package infra.persistence.query;

import org.junit.jupiter.api.Test;

import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.annotation.Trim;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@link ValueNormalizer} behaviour: the shared {@link ValueNormalizer#DEFAULT}
 * trims the string value of a property annotated with {@link Trim @Trim} and leaves
 * every other value unchanged.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ValueNormalizerTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  final EntityMetadata metadata = metadataFactory.getEntityMetadata(Model.class);

  final EntityProperty trimmed = metadata.findProperty("trimmed");

  final EntityProperty plain = metadata.findProperty("plain");

  final EntityProperty number = metadata.findProperty("number");

  @Test
  void trimsStringValueOfTrimAnnotatedProperty() {
    assertThat(ValueNormalizer.DEFAULT.normalize(trimmed, "  TODAY  ")).isEqualTo("TODAY");
  }

  @Test
  void leavesTrimAnnotatedPropertyNonStringUnchanged() {
    Object value = new Object();
    assertThat(ValueNormalizer.DEFAULT.normalize(trimmed, value)).isSameAs(value);
  }

  @Test
  void leavesPlainStringValueUnchanged() {
    assertThat(ValueNormalizer.DEFAULT.normalize(plain, "  TODAY  ")).isEqualTo("  TODAY  ");
  }

  @Test
  void leavesNumberValueUnchanged() {
    assertThat(ValueNormalizer.DEFAULT.normalize(number, 42)).isEqualTo(42);
  }

  static class Model {

    @Trim
    public String trimmed;

    public String plain;

    public int number;

  }

}
