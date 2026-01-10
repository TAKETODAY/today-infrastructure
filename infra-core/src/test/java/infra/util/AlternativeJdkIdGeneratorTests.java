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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:53
 */
class AlternativeJdkIdGeneratorTests {

  @Test
  void generateIdCreatesNewUUIDEachTime() {
    AlternativeJdkIdGenerator generator = new AlternativeJdkIdGenerator();

    UUID id1 = generator.generateId();
    UUID id2 = generator.generateId();

    assertThat(id1).isNotNull();
    assertThat(id2).isNotNull();
    assertThat(id1).isNotEqualTo(id2);
  }

  @Test
  void multipleInstancesGenerateDifferentUUIDs() {
    AlternativeJdkIdGenerator generator1 = new AlternativeJdkIdGenerator();
    AlternativeJdkIdGenerator generator2 = new AlternativeJdkIdGenerator();

    UUID id1 = generator1.generateId();
    UUID id2 = generator2.generateId();

    assertThat(id1).isNotEqualTo(id2);
  }

  @Test
  void generatedUUIDsAreUnique() {
    AlternativeJdkIdGenerator generator = new AlternativeJdkIdGenerator();
    Set<UUID> ids = new HashSet<>();

    for (int i = 0; i < 1000; i++) {
      assertThat(ids.add(generator.generateId())).isTrue();
    }
  }

  @Test
  void uuidBitsAreRandomlyDistributed() {
    AlternativeJdkIdGenerator generator = new AlternativeJdkIdGenerator();
    UUID uuid = generator.generateId();

    assertThat(uuid.getMostSignificantBits()).isNotZero();
    assertThat(uuid.getLeastSignificantBits()).isNotZero();
    assertThat(Long.bitCount(uuid.getMostSignificantBits())).isBetween(1, 63);
    assertThat(Long.bitCount(uuid.getLeastSignificantBits())).isBetween(1, 63);
  }

}