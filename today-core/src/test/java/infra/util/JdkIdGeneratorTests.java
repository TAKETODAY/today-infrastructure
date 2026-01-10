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