/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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