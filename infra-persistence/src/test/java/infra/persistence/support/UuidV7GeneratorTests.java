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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UuidV7GeneratorTests {

  @Test
  void encodesTimestampVersionAndVariant() {
    for (long timestamp : new long[] { 0, 0x0123456789ABL, 0xFFFFFFFFFFFFL }) {
      UUID uuid = UuidV7Generator.generate(timestamp);
      assertThat(uuid.version()).isEqualTo(7);
      assertThat(uuid.variant()).isEqualTo(2);
      assertThat(uuid.getMostSignificantBits() >>> 16).isEqualTo(timestamp);
      assertThat(UUID.fromString(uuid.toString())).isEqualTo(uuid);
    }
  }

  @Test
  void sameMillisecondConcurrentGenerationProducesDistinctIds() {
    var ids = IntStream.range(0, 10_000).parallel()
            .mapToObj(index -> UuidV7Generator.generate(1_700_000_000_000L)).toList();
    assertThat(ids).doesNotHaveDuplicates();
  }

  @Test
  void clockRollbackIsReflectedInTimestamp() {
    UUID before = UuidV7Generator.generate(1_700_000_000_100L);
    UUID after = UuidV7Generator.generate(1_700_000_000_000L);
    assertThat(after.getMostSignificantBits() >>> 16).isEqualTo(1_700_000_000_000L);
    assertThat(after.getMostSignificantBits() >>> 16).isLessThan(before.getMostSignificantBits() >>> 16);
  }

  @Test
  void rejectsTimestampsOutsideUnsigned48BitRange() {
    assertThatThrownBy(() -> UuidV7Generator.generate(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UuidV7Generator.generate(0x1000000000000L)).isInstanceOf(IllegalArgumentException.class);
  }

}
