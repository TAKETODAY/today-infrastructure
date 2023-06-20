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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatObject;

/**
 * Tests for {@link SourceFiles}.
 *
 * @author Phillip Webb
 */
class SourceFilesTests {

  private static final SourceFile SOURCE_FILE_1 = SourceFile.of(
          "public class Test1 {}");

  private static final SourceFile SOURCE_FILE_2 = SourceFile.of(
          "public class Test2 {}");

  @Test
  void noneReturnsNone() {
    SourceFiles none = SourceFiles.none();
    assertThat(none).isNotNull();
    assertThat(none.isEmpty()).isTrue();
  }

  @Test
  void ofCreatesSourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
    assertThat(sourceFiles).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
  }

  @Test
  void andAddsSourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
    SourceFiles added = sourceFiles.and(SOURCE_FILE_2);
    assertThat(sourceFiles).containsExactly(SOURCE_FILE_1);
    assertThat(added).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
  }

  @Test
  void andSourceFilesAddsSourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
    SourceFiles added = sourceFiles.and(SourceFiles.of(SOURCE_FILE_2));
    assertThat(sourceFiles).containsExactly(SOURCE_FILE_1);
    assertThat(added).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
  }

  @Test
  void iteratorIteratesSourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
    Iterator<SourceFile> iterator = sourceFiles.iterator();
    assertThat(iterator.next()).isEqualTo(SOURCE_FILE_1);
    assertThat(iterator.next()).isEqualTo(SOURCE_FILE_2);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void streamStreamsSourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
    assertThat(sourceFiles.stream()).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
  }

  @Test
  void isEmptyWhenEmptyReturnsTrue() {
    SourceFiles sourceFiles = SourceFiles.of();
    assertThat(sourceFiles.isEmpty()).isTrue();
  }

  @Test
  void isEmptyWhenNotEmptyReturnsFalse() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
    assertThat(sourceFiles.isEmpty()).isFalse();
  }

  @Test
  void getWhenHasFileReturnsFile() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
    assertThat(sourceFiles.get("Test1.java")).isNotNull();
  }

  @Test
  void getWhenMissingFileReturnsNull() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_2);
    assertThatObject(sourceFiles.get("Test1.java")).isNull();
  }

  @Test
  void getSingleWhenHasNoFilesThrowsException() {
    assertThatIllegalStateException().isThrownBy(
            () -> SourceFiles.none().getSingle());
  }

  @Test
  void getSingleWhenHasMultipleFilesThrowsException() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
    assertThatIllegalStateException().isThrownBy(sourceFiles::getSingle);
  }

  @Test
  void getSingleWhenHasSingleFileReturnsFile() {
    SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
    assertThat(sourceFiles.getSingle()).isEqualTo(SOURCE_FILE_1);
  }

  @Test
  void equalsAndHashCode() {
    SourceFiles s1 = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
    SourceFiles s2 = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
    SourceFiles s3 = SourceFiles.of(SOURCE_FILE_1);
    assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    assertThatObject(s1).isEqualTo(s2).isNotEqualTo(s3);
  }

}
