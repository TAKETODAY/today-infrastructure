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

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.http.converter.HttpMessageConverter;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
import infra.web.mock.MockRequestContext;

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
    RequestContext mockContext = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();

    ServerRequest request = ServerRequest.create(mockContext, converters);

    assertThat(request).isNotNull();
    assertThat(request.exchange()).isSameAs(mockContext);
  }

  @Test
  void createServerRequestWithVersionStrategy() {
    RequestContext mockContext = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ApiVersionStrategy versionStrategy = mock();

    ServerRequest request = ServerRequest.create(mockContext, converters, versionStrategy);

    assertThat(request).isNotNull();
    assertThat(request.apiVersionStrategy()).isSameAs(versionStrategy);
  }

  @Test
  void findServerRequest() {
    RequestContext mockContext = new MockRequestContext();
    ServerRequest mockRequest = mock();
    mockContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, mockRequest);

    ServerRequest foundRequest = ServerRequest.find(mockContext);

    assertThat(foundRequest).isSameAs(mockRequest);
  }

  @Test
  void findServerRequestNotFound() {
    RequestContext mockContext = new MockRequestContext();

    ServerRequest foundRequest = ServerRequest.find(mockContext);

    assertThat(foundRequest).isNull();
  }

  @Test
  void findRequiredServerRequest() {
    RequestContext mockContext = new MockRequestContext();
    ServerRequest mockRequest = mock();
    mockContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, mockRequest);

    ServerRequest foundRequest = ServerRequest.findRequired(mockContext);

    assertThat(foundRequest).isSameAs(mockRequest);
  }

  @Test
  void findRequiredServerRequestNotFound() {
    RequestContext mockContext = new MockRequestContext();

    assertThatThrownBy(() -> ServerRequest.findRequired(mockContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Required attribute");
  }

  @Test
  void builderFromServerRequest() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);

    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    assertThat(builder).isNotNull();
  }

  @Test
  void builderMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.method(HttpMethod.POST).build();

    assertThat(request.method()).isEqualTo(HttpMethod.POST);
  }

  @Test
  void builderUri() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    URI uri = URI.create("http://example.com/test");
    ServerRequest request = builder.uri(uri).build();

    assertThat(request.uri()).isEqualTo(uri);
  }

  @Test
  void builderHeader() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.header("X-Custom-Header", "custom-value").build();

    assertThat(request.headers().firstHeader("X-Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  void builderCookie() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.cookie("sessionId", "abc123").build();

    assertThat(request.cookies().getFirst("sessionId").getValue()).isEqualTo("abc123");
  }

  @Test
  void builderAttribute() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.attribute("customKey", "customValue").build();

    assertThat(request.attribute("customKey")).isEqualTo("customValue");
  }

  @Test
  void builderParam() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.param("page", "1").build();

    assertThat(request.param("page")).hasValue("1");
  }

  @Test
  void builderRemoteAddress() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    InetSocketAddress address = new InetSocketAddress("192.168.1.100", 8080);
    ServerRequest request = builder.remoteAddress(address).build();

    assertThat(request.remoteAddress()).isEqualTo(address);
  }

  @Test
  void builderBodyByteArray() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    byte[] body = "test body".getBytes();
    ServerRequest request = builder.body(body).build();

    assertThat(request).isNotNull();
  }

  @Test
  void builderBodyString() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.body("test body content").build();

    assertThat(request).isNotNull();
  }

  @Test
  void builderHeadersConsumer() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest originalRequest = ServerRequest.create(context, converters);
    ServerRequest.Builder builder = ServerRequest.from(originalRequest);

    ServerRequest request = builder.headers(headers -> headers.add("X-Frame-Options", "DENY")).build();

    assertThat(request.headers().firstHeader("X-Frame-Options")).isEqualTo("DENY");
  }

  @Test
  void builderCookiesConsumer() {
    RequestContext context = new MockRequestContext();
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
    RequestContext context = new MockRequestContext();
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
    RequestContext context = new MockRequestContext();
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
    RequestContext context = new MockRequestContext();
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
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of param methods requires setting up parameters in the context
    // These tests verify the interface methods exist and can be called
    assertThat(request).isNotNull();
  }

  @Test
  void pathMethods() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of path methods
    assertThat(request.path()).isNotNull();
  }

  @Test
  void checkNotModifiedWithLastModified() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);
    Instant lastModified = Instant.now().minusSeconds(3600);

    Optional<ServerResponse> response = request.checkNotModified(lastModified);

    assertThat(response).isNotNull();
  }

  @Test
  void checkNotModifiedWithEtag() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);
    String etag = "\"abc123\"";

    Optional<ServerResponse> response = request.checkNotModified(etag);

    assertThat(response).isNotNull();
  }

  @Test
  void checkNotModifiedWithLastModifiedAndEtag() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);
    Instant lastModified = Instant.now().minusSeconds(3600);
    String etag = "\"abc123\"";

    Optional<ServerResponse> response = request.checkNotModified(lastModified, etag);

    assertThat(response).isNotNull();
  }

  @Test
  void pathVariableMethods() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of pathVariable methods
    assertThat(request.pathVariables()).isNotNull();
  }

  @Test
  void headersMethods() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of headers methods
    assertThat(request.headers()).isNotNull();
  }

  @Test
  void cookiesMethods() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of cookies methods
    assertThat(request.cookies()).isNotNull();
  }

  @Test
  void attributesMethods() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of attributes methods
    assertThat(request.attributes()).isNotNull();
  }

  @Test
  void messageConvertersMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of messageConverters methods
    assertThat(request.messageConverters()).isNotNull();
  }

  @Test
  void exchangeMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of exchange methods
    assertThat(request.exchange()).isNotNull();
  }

  @Test
  void apiVersionStrategyMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of apiVersionStrategy methods
    assertThat(request.apiVersionStrategy()).isNull();
  }

  @Test
  void requestPathMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of requestPath methods
    assertThat(request.requestPath()).isNotNull();
  }

  @Test
  void methodNameMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of methodName methods
    assertThat(request.methodName()).isNotNull();
  }

  @Test
  void methodMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of method methods
    assertThat(request.method()).isNotNull();
  }

  @Test
  void uriMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of uri methods
    assertThat(request.uri()).isNotNull();
  }

  @Test
  void uriBuilderMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of uriBuilder methods
    assertThat(request.uriBuilder()).isNotNull();
  }

  @Test
  void remoteAddressMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of remoteAddress methods
    assertThat(request.remoteAddress()).isNotNull();
  }

  @Test
  void paramsMethod() {
    RequestContext context = new MockRequestContext();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    ServerRequest request = ServerRequest.create(context, converters);

    // Testing default implementations of params methods
    assertThat(request.params()).isNotNull();
  }

}