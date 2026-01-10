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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.http.converter.HttpMessageConverter;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 16:51
 */
class ErrorHandlingServerResponseTests {

  @Test
  void handleErrorWithoutMatchingHandler() throws Throwable {
    TestErrorHandlingServerResponse response = new TestErrorHandlingServerResponse();
    ServerRequest serverRequest = mock();
    RequestContext requestContext = new MockRequestContext();
    requestContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);

    response.addErrorHandler(
            throwable -> throwable instanceof IllegalStateException,
            (throwable, request) -> ServerResponse.badRequest().build()
    );

    Throwable testException = new IllegalArgumentException("test");

    assertThatThrownBy(() -> response.handleError(testException, requestContext, new ServerResponse.Context() {
      @Override
      public java.util.List<HttpMessageConverter<?>> messageConverters() {
        return new java.util.ArrayList<>();
      }
    })).isSameAs(testException);
  }

  @Test
  void errorResponseWithMatchingHandler() {
    TestErrorHandlingServerResponse response = new TestErrorHandlingServerResponse();
    ServerRequest serverRequest = mock();
    RequestContext requestContext = new MockRequestContext();
    requestContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);

    ServerResponse expectedResponse = ServerResponse.badRequest().build();
    response.addErrorHandler(
            throwable -> throwable instanceof IllegalArgumentException,
            (throwable, request) -> expectedResponse
    );

    Throwable testException = new IllegalArgumentException("test");
    ServerResponse result = response.errorResponse(testException, requestContext);

    assertThat(result).isSameAs(expectedResponse);
  }

  @Test
  void errorResponseWithoutMatchingHandler() {
    TestErrorHandlingServerResponse response = new TestErrorHandlingServerResponse();
    ServerRequest serverRequest = mock();
    RequestContext requestContext = new MockRequestContext();
    requestContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);

    response.addErrorHandler(
            throwable -> throwable instanceof IllegalStateException,
            (throwable, request) -> ServerResponse.badRequest().build()
    );

    Throwable testException = new IllegalArgumentException("test");
    ServerResponse result = response.errorResponse(testException, requestContext);

    assertThat(result).isNull();
  }

  @Test
  void errorResponseWithoutAnyHandlers() {
    TestErrorHandlingServerResponse response = new TestErrorHandlingServerResponse();
    ServerRequest serverRequest = mock();
    RequestContext requestContext = new MockRequestContext();
    requestContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);

    Throwable testException = new IllegalArgumentException("test");
    ServerResponse result = response.errorResponse(testException, requestContext);

    assertThat(result).isNull();
  }

  static class TestErrorHandlingServerResponse extends ErrorHandlingServerResponse {

    @Override
    public HttpStatusCode statusCode() {
      return null;
    }

    @Override
    public int rawStatusCode() {
      return 0;
    }

    @Override
    public HttpHeaders headers() {
      return null;
    }

    @Override
    public MultiValueMap<String, ResponseCookie> cookies() {
      return null;
    }

    @Override
    public @Nullable Object writeTo(RequestContext request, Context context) throws Throwable {
      return null;
    }
  }

}