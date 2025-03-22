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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:52
 */
class JdkIdGeneratorTests {

  @Test
  void generateIdCreatesNewUUIDEachTime() {
    JdkIdGenerator generator = new JdkIdGenerator();

    UUID id1 = generator.generateId();
    UUID id2 = generator.generateId();

    assertThat(id1).isNotNull();
    assertThat(id2).isNotNull();
    assertThat(id1).isNotEqualTo(id2);
  }

  @Test
  void multipleInstancesGenerateDifferentUUIDs() {
    JdkIdGenerator generator1 = new JdkIdGenerator();
    JdkIdGenerator generator2 = new JdkIdGenerator();

    UUID id1 = generator1.generateId();
    UUID id2 = generator2.generateId();

    assertThat(id1).isNotEqualTo(id2);
  }

  @Test
  void generatedUUIDsAreRandomVersion4() {
    JdkIdGenerator generator = new JdkIdGenerator();
    UUID id = generator.generateId();

    // Version 4 UUID has version bits set to 0100 (4)
    assertThat(id.version()).isEqualTo(4);
    // Variant bits set to 10xx
    assertThat(id.variant()).isEqualTo(2);
  }

}