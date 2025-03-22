/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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