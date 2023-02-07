/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link TextMessage}.
 *
 * @author Shinobu Aoki
 * @author Juergen Hoeller
 */
public class TextMessageTests {

  @Test
  public void toStringWithAscii() {
    String expected = "foo,bar";
    TextMessage actual = new TextMessage(expected);
    assertThat(actual.getPayload()).isEqualTo(expected);
    assertThat(actual.toString()).contains(expected);
  }

  @Test
  public void toStringWithMultibyteString() {
    String expected = "\u3042\u3044\u3046\u3048\u304a";
    TextMessage actual = new TextMessage(expected);
    assertThat(actual.getPayload()).isEqualTo(expected);
    assertThat(actual.toString()).contains(expected);
  }

}
