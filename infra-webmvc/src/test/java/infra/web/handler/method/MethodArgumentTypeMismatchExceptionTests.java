/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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