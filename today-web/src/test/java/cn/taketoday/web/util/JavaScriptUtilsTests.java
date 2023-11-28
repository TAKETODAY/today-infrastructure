/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link JavaScriptUtils}.
 *
 * @author Rossen Stoyanchev
 */
public class JavaScriptUtilsTests {

  @Test
  public void escape() {
    StringBuilder sb = new StringBuilder();
    sb.append('"');
    sb.append('\'');
    sb.append('\\');
    sb.append('/');
    sb.append('\t');
    sb.append('\n');
    sb.append('\r');
    sb.append('\f');
    sb.append('\b');
    sb.append('\013');
    assertThat(JavaScriptUtils.javaScriptEscape(sb.toString())).isEqualTo("\\\"\\'\\\\\\/\\t\\n\\n\\f\\b\\v");
  }

  // SPR-9983

  @Test
  public void escapePsLsLineTerminators() {
    StringBuilder sb = new StringBuilder();
    sb.append('\u2028');
    sb.append('\u2029');
    String result = JavaScriptUtils.javaScriptEscape(sb.toString());

    assertThat(result).isEqualTo("\\u2028\\u2029");
  }

  // SPR-9983

  @Test
  public void escapeLessThanGreaterThanSigns() throws UnsupportedEncodingException {
    assertThat(JavaScriptUtils.javaScriptEscape("<>")).isEqualTo("\\u003C\\u003E");
  }

}
