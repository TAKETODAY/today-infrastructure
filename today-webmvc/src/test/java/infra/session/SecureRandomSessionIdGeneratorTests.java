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

package infra.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/3 16:46
 */
class SecureRandomSessionIdGeneratorTests {

  @Test
  void test() {
    var generator = new SecureRandomSessionIdGenerator();
    generator.setSessionIdLength(16);
    assertThat(generator.generateId()).hasSize(16);
  }

  @Test
  void generateIdProducesUniqueValues() {
    var generator = new SecureRandomSessionIdGenerator();
    generator.setSessionIdLength(16);

    String id1 = generator.generateId();
    String id2 = generator.generateId();

    assertThat(id1).isNotEqualTo(id2);
  }

  @Test
  void defaultLengthIs15() {
    var generator = new SecureRandomSessionIdGenerator();

    String id = generator.generateId();

    assertThat(id).hasSize(20); // 15 bytes = 20 base64 chars
  }

  @Test
  void sessionIdLengthCanBeSet() {
    var generator = new SecureRandomSessionIdGenerator();

    generator.setSessionIdLength(8);
    String id1 = generator.generateId();

    generator.setSessionIdLength(32);
    String id2 = generator.generateId();

    assertThat(id1).hasSize(8);
    assertThat(id2).hasSize(32);
  }

  @Test
  void generatedIdsContainOnlyValidCharacters() {
    var generator = new SecureRandomSessionIdGenerator();
    generator.setSessionIdLength(24);

    String id = generator.generateId();

    assertThat(id).matches("^[A-Za-z0-9\\-_]+$");
  }

  @Test
  void multipleGenerationsProduceDifferentResults() {
    var generator = new SecureRandomSessionIdGenerator();
    generator.setSessionIdLength(16);

    var ids = new java.util.HashSet<String>();
    for (int i = 0; i < 100; i++) {
      ids.add(generator.generateId());
    }

    assertThat(ids).hasSize(100);
  }

  @Test
  void settingZeroLengthThrowsException() {
    var generator = new SecureRandomSessionIdGenerator();

    assertThatThrownBy(() -> generator.setSessionIdLength(0))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void settingNegativeLengthThrowsException() {
    var generator = new SecureRandomSessionIdGenerator();

    assertThatThrownBy(() -> generator.setSessionIdLength(-1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void encodeProducesConsistentOutput() {
    var generator = new SecureRandomSessionIdGenerator();

    // Using reflection to access the encode method
    try {
      java.lang.reflect.Method encodeMethod = SecureRandomSessionIdGenerator.class.getDeclaredMethod("encode", byte[].class);
      encodeMethod.setAccessible(true);

      byte[] testData = "test".getBytes();
      char[] result = (char[]) encodeMethod.invoke(generator, (Object) testData);

      assertThat(new String(result)).hasSize(8); // 4 bytes = 8 base64 chars
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void constructorInitializesRandomCorrectly() {
    var generator1 = new SecureRandomSessionIdGenerator();
    var generator2 = new SecureRandomSessionIdGenerator();

    String id1 = generator1.generateId();
    String id2 = generator2.generateId();

    assertThat(id1).isNotEqualTo(id2);
  }

}