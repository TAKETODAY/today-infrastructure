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
 * Tests for {@link ResourceFilesTests}.
 *
 * @author Phillip Webb
 */
class ResourceFilesTests {

  private static final ResourceFile RESOURCE_FILE_1 = ResourceFile.of("path1",
          "resource1");

  private static final ResourceFile RESOURCE_FILE_2 = ResourceFile.of("path2",
          "resource2");

  @Test
  void noneReturnsNone() {
    ResourceFiles none = ResourceFiles.none();
    assertThat(none).isNotNull();
    assertThat(none.isEmpty()).isTrue();
  }

  @Test
  void ofCreatesResourceFiles() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
    assertThat(resourceFiles).containsExactly(RESOURCE_FILE_1, RESOURCE_FILE_2);
  }

  @Test
  void andAddsResourceFiles() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
    ResourceFiles added = resourceFiles.and(RESOURCE_FILE_2);
    assertThat(resourceFiles).containsExactly(RESOURCE_FILE_1);
    assertThat(added).containsExactly(RESOURCE_FILE_1, RESOURCE_FILE_2);
  }

  @Test
  void andResourceFilesAddsResourceFiles() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
    ResourceFiles added = resourceFiles.and(ResourceFiles.of(RESOURCE_FILE_2));
    assertThat(resourceFiles).containsExactly(RESOURCE_FILE_1);
    assertThat(added).containsExactly(RESOURCE_FILE_1, RESOURCE_FILE_2);
  }

  @Test
  void iteratorIteratesResourceFiles() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
    Iterator<ResourceFile> iterator = resourceFiles.iterator();
    assertThat(iterator.next()).isEqualTo(RESOURCE_FILE_1);
    assertThat(iterator.next()).isEqualTo(RESOURCE_FILE_2);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void streamStreamsResourceFiles() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
    assertThat(resourceFiles.stream()).containsExactly(RESOURCE_FILE_1,
            RESOURCE_FILE_2);
  }

  @Test
  void isEmptyWhenEmptyReturnsTrue() {
    ResourceFiles resourceFiles = ResourceFiles.of();
    assertThat(resourceFiles.isEmpty()).isTrue();
  }

  @Test
  void isEmptyWhenNotEmptyReturnsFalse() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
    assertThat(resourceFiles.isEmpty()).isFalse();
  }

  @Test
  void getWhenHasFileReturnsFile() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
    assertThat(resourceFiles.get("path1")).isNotNull();
  }

  @Test
  void getWhenMissingFileReturnsNull() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_2);
    assertThatObject(resourceFiles.get("path1")).isNull();
  }

  @Test
  void getSingleWhenHasNoFilesThrowsException() {
    assertThatIllegalStateException().isThrownBy(
            () -> ResourceFiles.none().getSingle());
  }

  @Test
  void getSingleWhenHasMultipleFilesThrowsException() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
    assertThatIllegalStateException().isThrownBy(resourceFiles::getSingle);
  }

  @Test
  void getSingleWhenHasSingleFileReturnsFile() {
    ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
    assertThat(resourceFiles.getSingle()).isEqualTo(RESOURCE_FILE_1);
  }

  @Test
  void equalsAndHashCode() {
    ResourceFiles s1 = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
    ResourceFiles s2 = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
    ResourceFiles s3 = ResourceFiles.of(RESOURCE_FILE_1);
    assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    assertThatObject(s1).isEqualTo(s2).isNotEqualTo(s3);
  }

}
