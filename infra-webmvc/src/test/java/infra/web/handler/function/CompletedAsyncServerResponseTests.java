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