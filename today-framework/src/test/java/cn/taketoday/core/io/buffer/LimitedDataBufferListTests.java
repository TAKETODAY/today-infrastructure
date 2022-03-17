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

package cn.taketoday.core.io.buffer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LimitedDataBufferList}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class LimitedDataBufferListTests {

  @Test
  void limitEnforced() {
    LimitedDataBufferList list = new LimitedDataBufferList(5);

    assertThatThrownBy(() -> list.add(toDataBuffer("123456"))).isInstanceOf(DataBufferLimitException.class);
    assertThat(list).isEmpty();
  }

  @Test
  void limitIgnored() {
    new LimitedDataBufferList(-1).add(toDataBuffer("123456"));
  }

  @Test
  void clearResetsCount() {
    LimitedDataBufferList list = new LimitedDataBufferList(5);
    list.add(toDataBuffer("12345"));
    list.clear();
    list.add(toDataBuffer("12345"));
  }

  private static DataBuffer toDataBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
  }

}
