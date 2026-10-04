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

package infra.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7GeneratorTests {

  private final UuidV7Generator generator = new UuidV7Generator();

  @Test
  void printOneHundredSequentialIds() {
    for (int i = 0; i < 100; i++) {
      System.out.println(generator.generateId());
    }
  }

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
    var ids = IntStream.range(0, 1_000).parallel()
            .mapToObj(index -> generator.generateId()).toList();

    assertThat(ids).hasSize(1_000).doesNotHaveDuplicates().allSatisfy(uuid -> {
      assertThat(uuid.version()).isEqualTo(7);
      assertThat(uuid.variant()).isEqualTo(2);
    });
  }

  @Test
  void sequentialGenerationProducesStrictlyIncreasingIds() {
    UUID previous = generator.generateId();
    for (int i = 0; i < 10_000; i++) {
      UUID next = generator.generateId();
      assertThat(next.toString()).isGreaterThan(previous.toString());
      previous = next;
    }
  }

  @Test
  void laterMillisecondTimeResetsSequence() {
    Instant timestamp = Instant.ofEpochSecond(1, 999_999);
    var state = new UuidV7Generator.State(timestamp, 100);
    Instant now = Instant.ofEpochSecond(1, 1_000_000);

    var next = state.next(now, 1);

    assertThat(next.timestamp()).isEqualTo(now);
    assertThat(next.subMillis()).isZero();
    assertThat(next.sequence()).isEqualTo(1);
  }

  @Test
  void laterSubMillisecondTimeResetsSequence() {
    Instant timestamp = Instant.ofEpochSecond(1, 100_000);
    var state = new UuidV7Generator.State(timestamp, 100);
    Instant now = timestamp.plusNanos(245);

    var next = state.next(now, 1);

    assertThat(next.timestamp()).isEqualTo(now);
    assertThat(next.subMillis()).isGreaterThan(state.subMillis());
    assertThat(next.sequence()).isEqualTo(1);
  }

  @Test
  void increasingSequencePreservesTimestampWithinSameTimeBucket() {
    Instant timestamp = Instant.ofEpochSecond(1);
    var state = new UuidV7Generator.State(timestamp, 100);

    var next = state.next(timestamp.plusNanos(1), 101);

    assertThat(next.timestamp()).isEqualTo(timestamp);
    assertThat(next.subMillis()).isEqualTo(state.subMillis());
    assertThat(next.sequence()).isEqualTo(101);
  }

  @Test
  void nonIncreasingSequenceAdvancesLogicalTimeEvenOnClockRollback() {
    Instant timestamp = Instant.ofEpochSecond(1, 100_000);
    var state = new UuidV7Generator.State(timestamp, 100);

    for (long sequence : new long[] { 99, 100 }) {
      var next = state.next(timestamp.minusSeconds(1), sequence);

      assertThat(next.timestamp()).isEqualTo(timestamp.plusNanos(245));
      assertThat(next.subMillis()).isGreaterThan(state.subMillis());
      assertThat(next.sequence()).isEqualTo(sequence);
    }
    assertThat(state.next(timestamp.minusSeconds(1), 101).timestamp()).isEqualTo(timestamp);
  }

  @Test
  void logicalTimeAdvanceCrossesMillisecondBoundary() {
    Instant timestamp = Instant.ofEpochSecond(1, 999_999);
    var state = new UuidV7Generator.State(timestamp, 100);

    var next = state.next(timestamp, 1);

    assertThat(state.subMillis()).isEqualTo(4095);
    assertThat(next.timestamp().toEpochMilli()).isEqualTo(timestamp.toEpochMilli() + 1);
    assertThat(next.subMillis()).isZero();
    assertThat(next.sequence()).isEqualTo(1);
  }

}
