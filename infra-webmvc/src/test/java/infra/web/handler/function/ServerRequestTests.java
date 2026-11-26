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

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.http.converter.HttpMessageConverter;
import infra.web.ServerHttpExchange;
import infra.web.accept.ApiVersionStrategy;
import infra.web.mock.MockServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 16:28
 */
class ServerRequestTests {

  @Test
  void createServerRequest() {
    ServerHttpExchange mockContext = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();

    ServerRequest request = ServerRequest.create(mockContext, converters);

    assertThat(request).isNotNull();
    assertThat(request.exchange()).isSameAs(mockContext);
  }

  @Test
  void createServerRequestWithVersionStrategy() {
    ServerHttpExchange mockContext = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ApiVersionStrategy versionStrategy = mock();

    ServerRequest request = ServerRequest.create(mockContext, converters, versionStrategy);

    assertThat(request).isNotNull();
    assertThat(request.apiVersionStrategy()).isSameAs(versionStrategy);
  }

  @Test
  void findServerRequest() {
    ServerHttpExchange mockContext = new MockServerHttpExchange();
    ServerRequest mockRequest = mock();
    mockContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, mockRequest);

    ServerRequest foundRequest = ServerRequest.find(mockContext);

    assertThat(foundRequest).isSameAs(mockRequest);
  }

  @Test
  void findServerRequestNotFound() {
    ServerHttpExchange mockContext = new MockServerHttpExchange();

    ServerRequest foundRequest = ServerRequest.find(mockContext);

    assertThat(foundRequest).isNull();
  }

  @Test
  void findRequiredServerRequest() {
    ServerHttpExchange mockContext = new MockServerHttpExchange();
    ServerRequest mockRequest = mock();
    mockContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, mockRequest);

    ServerRequest foundRequest = ServerRequest.findRequired(mockContext);

    assertThat(foundRequest).isSameAs(mockRequest);
  }

  @Test
  void findRequiredServerRequestNotFound() {
    ServerHttpExchange mockContext = new MockServerHttpExchange();

    assertThatThrownBy(() -> ServerRequest.findRequired(mockContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Required attribute");
  }

  @Test
  void builderFromServerRequest() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);

    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    assertThat(builder).isNotNull();
  }

  @Test
  void builderMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.method(HttpMethod.POST).build();

    assertThat(request.method()).isEqualTo(HttpMethod.POST);
  }

  @Test
  void builderUri() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    URI uri = URI.create("http://example.com/test");
    ServerRequest request = builder.uri(uri).build();

    assertThat(request.uri()).isEqualTo(uri);
  }

  @Test
  void builderHeader() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.header("X-Custom-Header", "custom-value").build();

    assertThat(request.headers().firstHeader("X-Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  void builderCookie() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.cookie("sessionId", "abc123").build();

    assertThat(request.cookies().getFirst("sessionId").getValue()).isEqualTo("abc123");
  }

  @Test
  void builderAttribute() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.attribute("customKey", "customValue").build();

    assertThat(request.attribute("customKey")).isEqualTo("customValue");
  }

  @Test
  void builderParam() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.param("page", "1").build();

    assertThat(request.param("page")).hasValue("1");
  }

  @Test
  void builderRemoteAddress() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    InetSocketAddress address = new InetSocketAddress("192.168.1.100", 8080);
    ServerRequest request = builder.remoteAddress(address).build();

    assertThat(request.remoteAddress()).isEqualTo(address);
  }

  @Test
  void builderBodyByteArray() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    byte[] body = "test body".getBytes();
    ServerRequest request = builder.body(body).build();

    assertThat(request).isNotNull();
  }

  @Test
  void builderBodyString() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.body("test body content").build();

    assertThat(request).isNotNull();
  }

  @Test
  void builderHeadersConsumer() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.headers(headers -> headers.add("X-Frame-Options", "DENY")).build();

    assertThat(request.headers().firstHeader("X-Frame-Options")).isEqualTo("DENY");
  }

  @Test
  void builderCookiesConsumer() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.cookies(cookies -> {
      cookies.add("session", new HttpCookie("session", "abc"));
      cookies.add("preferences", new HttpCookie("preferences", "dark-mode"));
    }).build();

    assertThat(request.cookies().getFirst("session").getValue()).isEqualTo("abc");
    assertThat(request.cookies().getFirst("preferences").getValue()).isEqualTo("dark-mode");
  }

  @Test
  void builderAttributesConsumer() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.attributes(attributes -> {
      attributes.put("key1", "value1");
      attributes.put("key2", "value2");
    }).build();

    assertThat(request.attribute("key1")).isEqualTo("value1");
    assertThat(request.attribute("key2")).isEqualTo("value2");
  }

  @Test
  void builderParamsConsumer() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.params(params -> {
      params.add("filter", "active");
      params.add("sort", "name");
    }).build();

    assertThat(request.params("filter")).containsExactly("active");
    assertThat(request.params("sort")).containsExactly("name");
  }

  @Test
  void builderChaining() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);
    URI uri = URI.create("http://example.com/api");
    InetSocketAddress address = new InetSocketAddress("192.168.1.100", 8080);

    ServerRequest request = builder
            .method(HttpMethod.PUT)
            .uri(uri)
            .header("Content-Type", "application/json")
            .cookie("auth", "token123")
            .attribute("userId", 12345)
            .param("version", "v1")
            .remoteAddress(address)
            .build();

    assertThat(request.method()).isEqualTo(HttpMethod.PUT);
    assertThat(request.uri()).isEqualTo(uri);
    assertThat(request.headers().firstHeader("Content-Type")).isEqualTo("application/json");
    assertThat(request.cookies().getFirst("auth").getValue()).isEqualTo("token123");
    assertThat(request.attribute("userId")).isEqualTo(12345);
    assertThat(request.param("version")).hasValue("v1");
    assertThat(request.remoteAddress()).isEqualTo(address);
  }

  @Test
  void paramMethods() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of param methods requires setting up parameters in the context
    // These tests verify the interface methods exist and can be called
    assertThat(request).isNotNull();
  }

  @Test
  void pathMethods() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of path methods
    assertThat(request.path()).isNotNull();
  }

  @Test
  void checkNotModifiedWithLastModified() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);
    Instant lastModified = Instant.now().minusSeconds(3600);

    Optional<ServerResponse> response = request.checkNotModified(lastModified);

    assertThat(response).isNotNull();
  }

  @Test
  void checkNotModifiedWithEtag() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);
    String etag = "\"abc123\"";

    Optional<ServerResponse> response = request.checkNotModified(etag);

    assertThat(response).isNotNull();
  }

  @Test
  void checkNotModifiedWithLastModifiedAndEtag() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);
    Instant lastModified = Instant.now().minusSeconds(3600);
    String etag = "\"abc123\"";

    Optional<ServerResponse> response = request.checkNotModified(lastModified, etag);

    assertThat(response).isNotNull();
  }

  @Test
  void pathVariableMethods() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of pathVariable methods
    assertThat(request.pathVariables()).isNotNull();
  }

  @Test
  void headersMethods() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of headers methods
    assertThat(request.headers()).isNotNull();
  }

  @Test
  void cookiesMethods() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of cookies methods
    assertThat(request.cookies()).isNotNull();
  }

  @Test
  void attributesMethods() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of attributes methods
    assertThat(request.attributes()).isNotNull();
  }

  @Test
  void messageConvertersMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of messageConverters methods
    assertThat(request.messageConverters()).isNotNull();
  }

  @Test
  void exchangeMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of exchange methods
    assertThat(request.exchange()).isNotNull();
  }

  @Test
  void apiVersionStrategyMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of apiVersionStrategy methods
    assertThat(request.apiVersionStrategy()).isNull();
  }

  @Test
  void requestPathMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of requestPath methods
    assertThat(request.requestPath()).isNotNull();
  }

  @Test
  void methodNameMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of methodName methods
    assertThat(request.methodAsString()).isNotNull();
  }

  @Test
  void methodMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of method methods
    assertThat(request.method()).isNotNull();
  }

  @Test
  void uriMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of uri methods
    assertThat(request.uri()).isNotNull();
  }

  @Test
  void uriBuilderMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of uriBuilder methods
    assertThat(request.uriBuilder()).isNotNull();
  }

  @Test
  void remoteAddressMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of remoteAddress methods
    assertThat(request.remoteAddress()).isNotNull();
  }

  @Test
  void paramsMethod() {
    ServerHttpExchange context = new MockServerHttpExchange();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of params methods
    assertThat(request.params()).isNotNull();
  }

}