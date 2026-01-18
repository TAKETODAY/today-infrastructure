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

package infra.http.reactive.server;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import reactor.netty.http.server.HttpServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/15 11:43
 */
class ReactorUriHelperTests {

  @Test
  public void hostnameWithZoneId() throws URISyntaxException {
    HttpServerRequest nettyRequest = mock();

    given(nettyRequest.scheme()).willReturn("http");
    given(nettyRequest.hostName()).willReturn("fe80::a%en1");
    given(nettyRequest.hostPort()).willReturn(80);
    given(nettyRequest.uri()).willReturn("/");

    URI uri = ReactorUriHelper.createUri(nettyRequest);
    assertThat(uri).hasScheme("http")
            .hasHost("[fe80::a%25en1]")
            .hasPort(-1)
            .hasPath("/")
            .hasToString("http://[fe80::a%25en1]/");
  }

}