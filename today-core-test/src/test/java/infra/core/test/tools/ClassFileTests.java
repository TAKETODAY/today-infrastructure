/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core.test.tools;

import org.junit.jupiter.api.Test;

import infra.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassFile}.
 *
 * @author Stephane Nicoll
 */
class ClassFileTests {

  private final static byte[] TEST_CONTENT = new byte[] { 'a' };

  @Test
  void ofNameAndByteArrayCreatesClass() {
    ClassFile classFile = ClassFile.of("com.example.Test", TEST_CONTENT);
    assertThat(classFile.getName()).isEqualTo("com.example.Test");
    assertThat(classFile.getContent()).isEqualTo(TEST_CONTENT);
  }

  @Test
  void ofNameAndInputStreamResourceCreatesClass() {
    ClassFile classFile = ClassFile.of("com.example.Test",
            new ByteArrayResource(TEST_CONTENT));
    assertThat(classFile.getName()).isEqualTo("com.example.Test");
    assertThat(classFile.getContent()).isEqualTo(TEST_CONTENT);
  }

  @Test
  void toClassNameWithPathToClassFile() {
    assertThat(ClassFile.toClassName("com/example/Test.class")).isEqualTo("com.example.Test");
  }

  @Test
  void toClassNameWithPathToTextFile() {
    assertThatIllegalArgumentException().isThrownBy(() -> ClassFile.toClassName("com/example/Test.txt"));
  }

}
