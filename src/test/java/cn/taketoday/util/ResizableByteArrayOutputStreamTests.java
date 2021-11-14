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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY 2021/8/21 01:28
 */
class ResizableByteArrayOutputStreamTests {

  private static final int INITIAL_CAPACITY = 256;

  private ResizableByteArrayOutputStream baos;

  private byte[] helloBytes;

  @BeforeEach
  void setUp() throws Exception {
    this.baos = new ResizableByteArrayOutputStream(INITIAL_CAPACITY);
    this.helloBytes = "Hello World".getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void resize() throws Exception {
    assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
    this.baos.write(helloBytes);
    int size = 64;
    this.baos.resize(size);
    assertThat(this.baos.capacity()).isEqualTo(size);
    assertByteArrayEqualsString(this.baos);
  }

  @Test
  void autoGrow() {
    assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
    for (int i = 0; i < 129; i++) {
      this.baos.write(0);
    }
    assertThat(this.baos.capacity()).isEqualTo(256);
  }

  @Test
  void grow() throws Exception {
    assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
    this.baos.write(helloBytes);
    this.baos.grow(1000);
    assertThat(this.baos.capacity()).isEqualTo((this.helloBytes.length + 1000));
    assertByteArrayEqualsString(this.baos);
  }

  @Test
  void write() throws Exception {
    this.baos.write(helloBytes);
    assertByteArrayEqualsString(this.baos);
  }

  @Test
  void failResize() throws Exception {
    this.baos.write(helloBytes);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.baos.resize(5));
  }

  private void assertByteArrayEqualsString(ResizableByteArrayOutputStream actual) {
    assertThat(actual.toByteArray()).isEqualTo(helloBytes);
  }

}
