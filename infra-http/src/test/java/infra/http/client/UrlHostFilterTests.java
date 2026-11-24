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

package infra.http.client;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link UrlHostFilter}.
 *
 * @author Phillip Webb
 */
class UrlHostFilterTests {

  @Test
  void throwIfFilteredWhenFilteredThrowsException() {
    UrlHostFilter filter = new UrlHostFilter(InetAddressFilter.externalAddresses());
    assertThatExceptionOfType(FilteredHostException.class)
      .isThrownBy(() -> filter.throwIfFiltered(new URI("http://localhost")));
  }

  @Test
  void throwIfFilteredWhenNotFilteredDoesNotThrowsException() {
    UrlHostFilter filter = new UrlHostFilter(InetAddressFilter.internalAddresses());
    assertThatNoException().isThrownBy(() -> filter.throwIfFiltered(new URI("http://localhost")));
  }

}
