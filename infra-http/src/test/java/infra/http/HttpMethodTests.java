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

package infra.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpMethodTests {

  @Test
  void valuesIncludeQueryAndReturnDefensiveCopy() {
    HttpMethod[] values = HttpMethod.values();
    assertThat(values).containsExactly(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE,
            HttpMethod.PATCH, HttpMethod.TRACE, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.CONNECT, HttpMethod.QUERY);
    values[0] = HttpMethod.POST;
    assertThat(HttpMethod.values()[0]).isEqualTo(HttpMethod.GET);
  }

  @Test
  void resolveRecognizesEveryDeclaredMethod() {
    for (HttpMethod method : HttpMethod.values()) {
      assertThat(HttpMethod.resolve(method.name())).isSameAs(method);
    }
  }
}
