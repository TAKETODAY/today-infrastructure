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

import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import javax.tools.JavaFileObject.Kind;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicClassFileObject}.
 *
 * @author Phillip Webb
 */
class DynamicClassFileObjectTests {

  @Test
  void getUriReturnsGeneratedUriBasedOnClassName() {
    DynamicClassFileObject fileObject = new DynamicClassFileObject("com.example.MyClass");
    assertThat(fileObject.toUri()).hasToString("class:///com/example/MyClass.class");
  }

  @Test
  void getKindReturnsClass() {
    DynamicClassFileObject fileObject = new DynamicClassFileObject("com.example.MyClass");
    assertThat(fileObject.getKind()).isEqualTo(Kind.CLASS);
  }

  @Test
  void openOutputStreamWritesToBytes() throws Exception {
    DynamicClassFileObject fileObject = new DynamicClassFileObject("com.example.MyClass");
    try (OutputStream outputStream = fileObject.openOutputStream()) {
      new ByteArrayInputStream("test".getBytes()).transferTo(outputStream);
    }
    assertThat(fileObject.getBytes()).isEqualTo("test".getBytes());
  }

}
