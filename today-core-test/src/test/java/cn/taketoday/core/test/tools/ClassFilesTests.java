/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.test.tools;

import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

/**
 * Tests for {@link ClassFiles}.
 *
 * @author Stephane Nicoll
 */
class ClassFilesTests {

  private static final ClassFile CLASS_FILE_1 = ClassFile.of(
          "com.example.Test1", new byte[] { 'a' });

  private static final ClassFile CLASS_FILE_2 = ClassFile.of(
          "com.example.Test2", new byte[] { 'b' });

  @Test
  void noneReturnsNone() {
    ClassFiles none = ClassFiles.none();
    assertThat(none).isNotNull();
    assertThat(none.isEmpty()).isTrue();
  }

  @Test
  void ofCreatesClassFiles() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
    assertThat(classFiles).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
  }

  @Test
  void andAddsClassFiles() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
    ClassFiles added = classFiles.and(CLASS_FILE_2);
    assertThat(classFiles).containsExactly(CLASS_FILE_1);
    assertThat(added).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
  }

  @Test
  void andClassFilesAddsClassFiles() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
    ClassFiles added = classFiles.and(ClassFiles.of(CLASS_FILE_2));
    assertThat(classFiles).containsExactly(CLASS_FILE_1);
    assertThat(added).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
  }

  @Test
  void iteratorIteratesClassFiles() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
    Iterator<ClassFile> iterator = classFiles.iterator();
    assertThat(iterator.next()).isEqualTo(CLASS_FILE_1);
    assertThat(iterator.next()).isEqualTo(CLASS_FILE_2);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void streamStreamsClassFiles() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
    assertThat(classFiles.stream()).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
  }

  @Test
  void isEmptyWhenEmptyReturnsTrue() {
    ClassFiles classFiles = ClassFiles.of();
    assertThat(classFiles.isEmpty()).isTrue();
  }

  @Test
  void isEmptyWhenNotEmptyReturnsFalse() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
    assertThat(classFiles.isEmpty()).isFalse();
  }

  @Test
  void getWhenHasFileReturnsFile() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
    assertThat(classFiles.get("com.example.Test1")).isNotNull();
  }

  @Test
  void getWhenMissingFileReturnsNull() {
    ClassFiles classFiles = ClassFiles.of(CLASS_FILE_2);
    assertThatObject(classFiles.get("com.example.another.Test2")).isNull();
  }

  @Test
  void equalsAndHashCode() {
    ClassFiles s1 = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
    ClassFiles s2 = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
    ClassFiles s3 = ClassFiles.of(CLASS_FILE_1);
    assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    assertThatObject(s1).isEqualTo(s2).isNotEqualTo(s3);
  }

}
