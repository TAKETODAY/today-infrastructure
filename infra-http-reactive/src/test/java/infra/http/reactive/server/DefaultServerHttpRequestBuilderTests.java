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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.support.Netty4HttpHeaders;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/11 21:17
 */
class DefaultServerHttpRequestBuilderTests {

  @Test
  void mutatesUriAndConnectionMetadataWithoutChangingOriginalRequest() {
    HttpCookie cookie = new HttpCookie("theme", "dark");
    ServerHttpRequest original = MockServerHttpRequest.post("/old/items").contextPath("/old")
            .header("X-Original", "original").cookie(cookie).body("payload");
    InetSocketAddress local = InetSocketAddress.createUnresolved("local", 8080);
    InetSocketAddress remote = InetSocketAddress.createUnresolved("remote", 12345);
    SslInfo sslInfo = mock(SslInfo.class);

    ServerHttpRequest mutated = original.mutate()
            .method(HttpMethod.PUT)
            .uri(URI.create("https://user%40example@example.org:8443/unused?q=a%2Fb#anchor"))
            .path("/new/item%20name")
            .contextPath("/new")
            .headers(headers -> headers.set("X-Original", "changed"))
            .sslInfo(sslInfo).localAddress(local).remoteAddress(remote)
            .build();

    assertThat(mutated.getURI()).hasToString("https://user%40example@example.org:8443/new/item%20name?q=a%2Fb#anchor");
    assertThat(mutated.getPath().contextPath().value()).isEqualTo("/new");
    assertThat(mutated.getPath().pathWithinApplication().value()).isEqualTo("/item%20name");
    assertThat(mutated.getQueryParams().getFirst("q")).isEqualTo("a/b");
    assertThat(mutated.getMethod()).isEqualTo(HttpMethod.PUT);
    assertThat(mutated.getHeaders().getFirst("X-Original")).isEqualTo("changed");
    assertThat(mutated.getSslInfo()).isSameAs(sslInfo);
    assertThat(mutated.getLocalAddress()).isEqualTo(local);
    assertThat(mutated.getRemoteAddress()).isEqualTo(remote);
    assertThat(mutated.getCookies().getFirst("theme")).isSameAs(cookie);
    assertThat(mutated.getBody()).isSameAs(original.getBody());
    assertThat(mutated.getId()).isEqualTo(original.getId());
    assertThat(original.getURI()).hasToString("/old/items");
    assertThat(original.getHeaders().getFirst("X-Original")).isEqualTo("original");
  }

  @Test
  void rewritesRelativePathAndClearsContextPath() {
    ServerHttpRequest original = MockServerHttpRequest.get("/old/items").contextPath("/old").build();

    ServerHttpRequest mutated = original.mutate().path("/items").contextPath(null).build();

    assertThat(mutated.getURI()).hasToString("/items");
    assertThat(mutated.getPath().contextPath().value()).isEmpty();
    assertThat(mutated.getQueryParams()).isEmpty();
    assertThat(mutated.getSslInfo()).isNull();
  }

  @Test
  void rejectsInvalidPaths() {
    ServerHttpRequest original = MockServerHttpRequest.get("/items").build();

    assertThatIllegalArgumentException().isThrownBy(() -> original.mutate().path("relative"))
            .withMessageContaining("leading slash");
    assertThatIllegalStateException().isThrownBy(() -> original.mutate().path("/item name").build())
            .withCauseInstanceOf(URISyntaxException.class);
  }

  @Test
  void requestDefaultsToUnavailableConnectionMetadata() {
    ServerHttpRequest request = mock(ServerHttpRequest.class, CALLS_REAL_METHODS);

    assertThat(request.getLocalAddress()).isNull();
    assertThat(request.getRemoteAddress()).isNull();
    assertThat(request.getSslInfo()).isNull();
  }

  @ParameterizedTest
  @MethodSource("headers")
  void containerImmutableHeadersAreCopied(HttpHeaders originalHeaders, boolean isMutableMap) {
    ServerHttpRequest mockRequest = createMockRequest(originalHeaders);
    final DefaultServerHttpRequestBuilder builder = new DefaultServerHttpRequestBuilder(mockRequest);

    //perform mutations on the map adapter of the container's headers if possible
    if (isMutableMap) {
      originalHeaders.setOrRemove("CaseInsensitive", "original");
      assertThat(originalHeaders.getFirst("caseinsensitive"))
              .as("original mutated")
              .isEqualTo("original");
    }
    else {
      assertThatRuntimeException().isThrownBy(() -> originalHeaders.setOrRemove("CaseInsensitive", "original"));
      assertThat(originalHeaders.getFirst("caseinsensitive"))
              .as("original not mutable")
              .isEqualTo("unmodified");
    }

    // Mutating the headers in the build. Note directly mutating via
    // .build().getHeaders() isn't applicable since/ headers are made
    // read-only by build()
    ServerHttpRequest req = builder
            .header("CaseInsensitive", "modified")
            .header("Additional", "header")
            .build();

    assertThat(req.getHeaders().getFirst("CaseInsensitive"))
            .as("copy mutated")
            .isEqualTo("modified");
    assertThat(req.getHeaders().getFirst("caseinsensitive"))
            .as("copy case-insensitive")
            .isEqualTo("modified");
    assertThat(req.getHeaders().getFirst("additional"))
            .as("copy has additional header")
            .isEqualTo("header");
  }

  private ServerHttpRequest createMockRequest(HttpHeaders originalHeaders) {
    //we can't use only use a MockServerHttpRequest because it uses a ReadOnlyHttpHeaders internally
    ServerHttpRequest mock = BDDMockito.spy(MockServerHttpRequest.get("/example").build());
    when(mock.getHeaders()).thenReturn(originalHeaders);

    return mock;
  }

  static Arguments initHeader(String description, HttpHeaders headerMap) {
    headerMap.add("CaseInsensitive", "unmodified");
    return argumentSet(description, headerMap, true);
  }

  static Stream<Arguments> headers() {
    return Stream.of(
            initHeader("Map", HttpHeaders.forWritable()),
            initHeader("Netty", new Netty4HttpHeaders(new DefaultHttpHeaders())),
            //immutable versions of some headers
            argumentSet("Netty immutable", new Netty4HttpHeaders(new ReadOnlyHttpHeaders(false,
                    "CaseInsensitive", "unmodified")), false)
    );
  }

}
