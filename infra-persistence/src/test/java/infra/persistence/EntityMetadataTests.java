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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import infra.persistence.annotation.Id;
import infra.persistence.annotation.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class EntityMetadataTests {

  private final DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

  @Table("t_synth")
  static class SynthEntity {
    @Id
    Long id;
  }

  @Test
  void synthesizedAnnotationResolvesAliasAttributes() {
    EntityMetadata metadata = factory.createEntityMetadata(SynthEntity.class);

    Table table = metadata.synthesizedAnnotation(Table.class);

    assertThat(table.value()).isEqualTo("t_synth");
    assertThat(table.name()).isEqualTo("t_synth");
  }

  @Test
  void synthesizedAnnotationThrowsWhenAnnotationMissing() {
    EntityMetadata metadata = factory.createEntityMetadata(SynthEntity.class);

    assertThatThrownBy(() -> metadata.synthesizedAnnotation(Deprecated.class))
            .isInstanceOf(NoSuchElementException.class);
  }

}