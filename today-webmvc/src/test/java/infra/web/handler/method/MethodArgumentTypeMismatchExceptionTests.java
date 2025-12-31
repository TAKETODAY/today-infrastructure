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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/7 20:06
 */
class MethodArgumentTypeMismatchExceptionTests {

  @Test
  void messageIncludesParameterName() {
    @SuppressWarnings("DataFlowIssue")
    MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "mismatched value", Integer.class, "paramOne", null, null);

    assertThat(ex).hasMessage("Method parameter 'paramOne': Failed to convert value of type " +
            "'java.lang.String' to required type 'java.lang.Integer'");
  }

}