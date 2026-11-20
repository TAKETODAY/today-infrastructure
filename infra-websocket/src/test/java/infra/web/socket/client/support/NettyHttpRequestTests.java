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

package infra.web.socket.client.support;

import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class NettyHttpRequestTests {

  @Test
  void exposesReadOnlyRequestData() {
    URI uri = URI.create("ws://localhost:8080/ws");
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Test", "value");
    NettyHttpRequest request = new NettyHttpRequest(uri, headers);

    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(request.getURI()).isEqualTo(uri);
    assertThat(request.getHeaders().getFirst("X-Test")).isEqualTo("value");
    assertThat(request.getHeaders()).isSameAs(request.getHeaders());
  }

}
