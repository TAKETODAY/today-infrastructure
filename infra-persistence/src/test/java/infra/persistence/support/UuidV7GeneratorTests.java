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

package infra.persistence.support;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7GeneratorTests {

  private final UuidV7Generator generator = new UuidV7Generator();

  @Test
  void encodesTimestampVersionAndVariant() {
    long before = System.currentTimeMillis();
    UUID uuid = generator.generateId();
    long after = System.currentTimeMillis();

    assertThat(uuid.version()).isEqualTo(7);
    assertThat(uuid.variant()).isEqualTo(2);
    assertThat(uuid.getMostSignificantBits() >>> 16).isBetween(before, after);
  }

  @Test
  void concurrentGenerationProducesDistinctVersion7Ids() {
    long before = System.currentTimeMillis();
    var ids = IntStream.range(0, 1_000).parallel()
            .mapToObj(index -> generator.generateId()).toList();
    long after = System.currentTimeMillis();

    assertThat(ids).hasSize(1_000).doesNotHaveDuplicates().allSatisfy(uuid -> {
      assertThat(uuid.version()).isEqualTo(7);
      assertThat(uuid.variant()).isEqualTo(2);
      assertThat(uuid.getMostSignificantBits() >>> 16).isBetween(before, after);
    });
  }

}
