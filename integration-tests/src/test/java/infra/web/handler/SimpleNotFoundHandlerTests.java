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

package infra.web.handler;

import org.junit.jupiter.api.Test;

import infra.http.HttpStatus;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.HttpRequestHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/11 17:45
 */
class SimpleNotFoundHandlerTests {
  SimpleNotFoundHandler notFoundHandler = new SimpleNotFoundHandler();

  MockRequest request = new MockRequest();
  MockResponse response = new MockResponse();

  @Test
  void handleNotFound() throws Throwable {
    request.setRequestURI("/not-found");
    MockHttpContext httpContext = new MockHttpContext(null, request, response);
    assertThat(notFoundHandler.handleNotFound(httpContext)).isEqualTo(HttpRequestHandler.NONE_RETURN_VALUE);
    assertThat(httpContext.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value()).isEqualTo(response.getStatus());
    assertThat(response.isCommitted()).isTrue();

  }

}