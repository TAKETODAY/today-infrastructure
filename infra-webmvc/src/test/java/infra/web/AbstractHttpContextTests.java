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

package infra.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractHttpContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class AbstractHttpContextTests {

  @Test
  void parsePortFromHostHeaderWithIpv4AndPort() {
    assertThat(AbstractHttpContext.parsePortFromHostHeader("example.com:8080")).isEqualTo(8080);
  }

  @Test
  void parsePortFromHostHeaderWithIpv4WithoutPort() {
    assertThat(AbstractHttpContext.parsePortFromHostHeader("example.com")).isEqualTo(-1);
  }

  @Test
  void parsePortFromHostHeaderWithIpv6AndPort() {
    assertThat(AbstractHttpContext.parsePortFromHostHeader("[::1]:9090")).isEqualTo(9090);
  }

  @Test
  void parsePortFromHostHeaderWithIpv6WithoutPort() {
    assertThat(AbstractHttpContext.parsePortFromHostHeader("[::1]")).isEqualTo(-1);
  }

  @Test
  void parsePortFromHostHeaderWithIpv6AndMalformedPort() {
    assertThat(AbstractHttpContext.parsePortFromHostHeader("[::1]:abc")).isEqualTo(-1);
  }

}
