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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/8/21 01:39
 */
class DigestUtilsTests {

  private byte[] bytes;

  @BeforeEach
  void createBytes() {
    bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void md5() throws IOException {
    byte[] expected = new byte[]
            { -0x4f, 0xa, -0x73, -0x4f, 0x64, -0x20, 0x75, 0x41, 0x5, -0x49, -0x57, -0x65, -0x19, 0x2e, 0x3f, -0x1b };

    byte[] result = DigestUtils.md5Digest(bytes);
    assertThat(result).as("Invalid hash").isEqualTo(expected);

    result = DigestUtils.md5Digest(new ByteArrayInputStream(bytes));
    assertThat(result).as("Invalid hash").isEqualTo(expected);
  }

  @Test
  void md5Hex() throws IOException {
    String expected = "b10a8db164e0754105b7a99be72e3fe5";

    String hash = DigestUtils.md5DigestAsHex(bytes);
    assertThat(hash).as("Invalid hash").isEqualTo(expected);

    hash = DigestUtils.md5DigestAsHex(new ByteArrayInputStream(bytes));
    assertThat(hash).as("Invalid hash").isEqualTo(expected);
  }

  @Test
  void md5StringBuilder() throws IOException {
    String expected = "b10a8db164e0754105b7a99be72e3fe5";

    StringBuilder builder = new StringBuilder();
    DigestUtils.appendMd5DigestAsHex(bytes, builder);
    assertThat(builder.toString()).as("Invalid hash").hasToString(expected);

    builder = new StringBuilder();
    DigestUtils.appendMd5DigestAsHex(new ByteArrayInputStream(bytes), builder);
    assertThat(builder.toString()).as("Invalid hash").hasToString(expected);
  }

}
