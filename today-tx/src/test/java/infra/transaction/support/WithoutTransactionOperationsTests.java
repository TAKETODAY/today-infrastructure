/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 21:27
 */
class WithoutTransactionOperationsTests {

  @Test
  void executeWithoutTransactionExecutesCallback() {
    Object result = WithoutTransactionOperations.INSTANCE.execute(status -> {
      assertThat(status.isNewTransaction()).isFalse();
      return "result";
    });
    assertThat(result).isEqualTo("result");
  }

  @Test
  void executeWithConfigurationIgnoresConfiguration() {
    Object result = WithoutTransactionOperations.INSTANCE.execute(
            status -> {
              assertThat(status.isNewTransaction()).isFalse();
              return "result";
            },
            new DefaultTransactionDefinition()
    );
    assertThat(result).isEqualTo("result");
  }

  @Test
  void executeWithNullConfigurationExecutesCallback() {
    Object result = WithoutTransactionOperations.INSTANCE.execute(
            status -> {
              assertThat(status.isNewTransaction()).isFalse();
              return "result";
            },
            null
    );
    assertThat(result).isEqualTo("result");
  }

  @Test
  void executeWithRuntimeExceptionPropagatesException() {
    RuntimeException expected = new RuntimeException("Test Exception");
    assertThatThrownBy(() ->
            WithoutTransactionOperations.INSTANCE.execute(status -> {
              throw expected;
            }))
            .isSameAs(expected);
  }

}