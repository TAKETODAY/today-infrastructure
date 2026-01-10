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
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HttpRequestHandler;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/11 17:45
 */
class SimpleNotFoundHandlerTests {
  SimpleNotFoundHandler notFoundHandler = new SimpleNotFoundHandler();

  HttpMockRequestImpl request = new HttpMockRequestImpl();
  MockHttpResponseImpl response = new MockHttpResponseImpl();

  @Test
  void handleNotFound() throws Throwable {
    request.setRequestURI("/not-found");
    MockRequestContext requestContext = new MockRequestContext(null, request, response);
    assertThat(notFoundHandler.handleNotFound(requestContext)).isEqualTo(HttpRequestHandler.NONE_RETURN_VALUE);
    assertThat(requestContext.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value()).isEqualTo(response.getStatus());
    assertThat(response.isCommitted()).isTrue();

  }

}