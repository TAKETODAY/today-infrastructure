/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.error;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import cn.taketoday.web.server.error.ErrorAttributeOptions;

import static cn.taketoday.web.server.error.ErrorAttributeOptions.Include;
import static cn.taketoday.web.server.error.ErrorAttributeOptions.of;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:13
 */
class ErrorAttributeOptionsTests {

  @Test
  void includingFromEmptyAttributesReturnAddedEntry() {
    ErrorAttributeOptions options = of(EnumSet.noneOf(Include.class));
    assertThat(options.including(Include.EXCEPTION).getIncludes()).containsOnly(Include.EXCEPTION);
  }

  @Test
  void includingFromMatchingAttributesDoesNotModifyOptions() {
    ErrorAttributeOptions options = of(EnumSet.of(Include.EXCEPTION, Include.STACK_TRACE));
    assertThat(options.including(Include.EXCEPTION).getIncludes()).containsOnly(Include.EXCEPTION,
            Include.STACK_TRACE);
  }

  @Test
  void excludingFromEmptyAttributesReturnEmptyList() {
    ErrorAttributeOptions options = of(EnumSet.noneOf(Include.class));
    assertThat(options.excluding(Include.EXCEPTION).getIncludes()).isEmpty();
  }

  @Test
  void excludingFromMatchingAttributesRemoveMatch() {
    ErrorAttributeOptions options = of(EnumSet.of(Include.EXCEPTION, Include.STACK_TRACE));
    assertThat(options.excluding(Include.EXCEPTION).getIncludes()).containsOnly(Include.STACK_TRACE);
  }

}
