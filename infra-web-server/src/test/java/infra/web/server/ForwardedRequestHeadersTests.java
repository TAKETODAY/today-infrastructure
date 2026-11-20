/*
 * Copyright 2012-present the original author or authors.
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

package infra.web.server;

import org.junit.jupiter.api.Test;

import infra.web.config.WebProperties.ForwardedHeaders.Naming;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ForwardedRequestHeadersTests {

  @Test
  @SuppressWarnings("NullAway")
  void createWhenNamingIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ForwardedRequestHeaders(null, false))
            .withMessage("Naming is required");
  }

  @Test
  void createWhenUsePrefixWithoutXForwardedNamingThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> new ForwardedRequestHeaders(Naming.RFC_7239, true))
            .withMessage("'usePrefix' can only be used with 'x-forwarded' naming");
  }

}
