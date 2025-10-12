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

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 16:47
 */
class CompletedAsyncServerResponseTests {

  @Test
  void blockReturnsWrappedResponse() {
    ServerResponse wrappedResponse = ServerResponse.ok().body("test");
    CompletedAsyncServerResponse asyncResponse = new CompletedAsyncServerResponse(wrappedResponse);

    ServerResponse result = asyncResponse.block();

    assertThat(result).isSameAs(wrappedResponse);
  }

  @Test
  void statusCodeDelegatesToWrappedResponse() {
    ServerResponse wrappedResponse = ServerResponse.notFound().build();
    CompletedAsyncServerResponse asyncResponse = new CompletedAsyncServerResponse(wrappedResponse);

    HttpStatusCode statusCode = asyncResponse.statusCode();

    assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void rawStatusCodeDelegatesToWrappedResponse() {
    ServerResponse wrappedResponse = ServerResponse.status(418).build();
    CompletedAsyncServerResponse asyncResponse = new CompletedAsyncServerResponse(wrappedResponse);

    int rawStatusCode = asyncResponse.rawStatusCode();

    assertThat(rawStatusCode).isEqualTo(418);
  }

  @Test
  void headersDelegatesToWrappedResponse() {
    ServerResponse wrappedResponse = ServerResponse.ok()
            .header("X-Custom-Header", "custom-value")
            .build();
    CompletedAsyncServerResponse asyncResponse = new CompletedAsyncServerResponse(wrappedResponse);

    HttpHeaders headers = asyncResponse.headers();

    assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  void cookiesDelegatesToWrappedResponse() {
    ServerResponse wrappedResponse = ServerResponse.ok()
            .cookie("sessionId", "abc123")
            .build();
    CompletedAsyncServerResponse asyncResponse = new CompletedAsyncServerResponse(wrappedResponse);

    MultiValueMap<String, ResponseCookie> cookies = asyncResponse.cookies();

    assertThat(cookies.getFirst("sessionId").getValue()).isEqualTo("abc123");
  }

  @Test
  void writeToDelegatesToWrappedResponse() throws Throwable {
    ServerResponse wrappedResponse = ServerResponse.ok().body("test content");
    CompletedAsyncServerResponse asyncResponse = new CompletedAsyncServerResponse(wrappedResponse);
    RequestContext requestContext = new MockRequestContext();

    Object result = asyncResponse.writeTo(requestContext, new ServerResponse.Context() {
      @Override
      public java.util.List<HttpMessageConverter<?>> messageConverters() {
        return List.of(new StringHttpMessageConverter());
      }
    });

    assertThat(result).isNotNull();
  }

}