/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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