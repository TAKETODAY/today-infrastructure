/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/8/21 00:07
 */
class FileCopyUtilsTests {

  @Test
  void copyFromInputStream() throws IOException {
    byte[] content = "content".getBytes();
    ByteArrayInputStream in = new ByteArrayInputStream(content);
    ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
    int count = FileCopyUtils.copy(in, out);
    assertThat(count).isEqualTo(content.length);
    assertThat(Arrays.equals(content, out.toByteArray())).isTrue();
  }

  @Test
  void copyFromByteArray() throws IOException {
    byte[] content = "content".getBytes();
    ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
    FileCopyUtils.copy(content, out);
    assertThat(Arrays.equals(content, out.toByteArray())).isTrue();
  }

  @Test
  void copyToByteArray() throws IOException {
    byte[] content = "content".getBytes();
    ByteArrayInputStream in = new ByteArrayInputStream(content);
    byte[] result = FileCopyUtils.copyToByteArray(in);
    assertThat(Arrays.equals(content, result)).isTrue();
  }

  @Test
  void copyFromReader() throws IOException {
    String content = "content";
    StringReader in = new StringReader(content);
    StringWriter out = new StringWriter();
    int count = FileCopyUtils.copy(in, out);
    assertThat(count).isEqualTo(content.length());
    assertThat(out.toString()).hasToString(content);
  }

  @Test
  void copyFromString() throws IOException {
    String content = "content";
    StringWriter out = new StringWriter();
    FileCopyUtils.copy(content, out);
    assertThat(out.toString()).hasToString(content);
  }

  @Test
  void copyToString() throws IOException {
    String content = "content";
    StringReader in = new StringReader(content);
    String result = FileCopyUtils.copyToString(in);
    assertThat(result).isEqualTo(content);
  }

}
