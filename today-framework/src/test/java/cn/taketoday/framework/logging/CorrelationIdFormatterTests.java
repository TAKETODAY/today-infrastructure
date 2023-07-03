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

package cn.taketoday.framework.logging;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 16:07
 */
class CorrelationIdFormatterTests {

  @Test
  void formatWithDefaultSpecWhenHasBothParts() {
    Map<String, String> context = new HashMap<>();
    context.put("traceId", "01234567890123456789012345678901");
    context.put("spanId", "0123456789012345");
    String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
    assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void formatWithDefaultSpecWhenHasNoParts() {
    Map<String, String> context = new HashMap<>();
    String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
    assertThat(formatted).isEqualTo("[                                                 ] ");
  }

  @Test
  void formatWithDefaultSpecWhenHasOnlyFirstPart() {
    Map<String, String> context = new HashMap<>();
    context.put("traceId", "01234567890123456789012345678901");
    String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
    assertThat(formatted).isEqualTo("[01234567890123456789012345678901-                ] ");
  }

  @Test
  void formatWithDefaultSpecWhenHasOnlySecondPart() {
    Map<String, String> context = new HashMap<>();
    context.put("spanId", "0123456789012345");
    String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
    assertThat(formatted).isEqualTo("[                                -0123456789012345] ");
  }

  @Test
  void formatWhenPartsAreShort() {
    Map<String, String> context = new HashMap<>();
    context.put("traceId", "0123456789012345678901234567");
    context.put("spanId", "012345678901");
    String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
    assertThat(formatted).isEqualTo("[0123456789012345678901234567    -012345678901    ] ");
  }

  @Test
  void formatWhenPartsAreLong() {
    Map<String, String> context = new HashMap<>();
    context.put("traceId", "01234567890123456789012345678901FFFF");
    context.put("spanId", "0123456789012345FFFF");
    String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
    assertThat(formatted).isEqualTo("[01234567890123456789012345678901FFFF-0123456789012345FFFF] ");
  }

  @Test
  void formatWithCustomSpec() {
    Map<String, String> context = new HashMap<>();
    context.put("a", "01234567890123456789012345678901");
    context.put("b", "0123456789012345");
    String formatted = CorrelationIdFormatter.of("a(32),b(16)").format(context::get);
    assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void formatToWithDefaultSpec() {
    Map<String, String> context = new HashMap<>();
    context.put("traceId", "01234567890123456789012345678901");
    context.put("spanId", "0123456789012345");
    StringBuilder formatted = new StringBuilder();
    CorrelationIdFormatter.DEFAULT.formatTo(context::get, formatted);
    assertThat(formatted).hasToString("[01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void ofWhenSpecIsMalformed() {
    assertThatIllegalStateException().isThrownBy(() -> CorrelationIdFormatter.of("good(12),bad"))
            .withMessage("Unable to parse correlation formatter spec 'good(12),bad'")
            .havingCause()
            .withMessage("Invalid specification part 'bad'");
  }

  @Test
  void ofWhenSpecIsEmpty() {
    assertThat(CorrelationIdFormatter.of("")).isSameAs(CorrelationIdFormatter.DEFAULT);
  }

  @Test
  void toStringReturnsSpec() {
    assertThat(CorrelationIdFormatter.DEFAULT).hasToString("traceId(32),spanId(16)");
    assertThat(CorrelationIdFormatter.of("a(32),b(16)")).hasToString("a(32),b(16)");
  }

}