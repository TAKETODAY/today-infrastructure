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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;

import java.util.Locale;
import java.util.stream.Stream;

import infra.http.HttpHeaders;
import infra.http.support.Netty4HttpHeaders;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/11 21:17
 */
class DefaultServerHttpRequestBuilderTests {

  @ParameterizedTest
  @MethodSource("headers")
  void containerImmutableHeadersAreCopied(MultiValueMap<String, String> headerMap, boolean isMutableMap) {
    HttpHeaders originalHeaders = HttpHeaders.forWritable(headerMap);
    ServerHttpRequest mockRequest = createMockRequest(originalHeaders);
    final DefaultServerHttpRequestBuilder builder = new DefaultServerHttpRequestBuilder(mockRequest);

    //perform mutations on the map adapter of the container's headers if possible
    if (isMutableMap) {
      headerMap.setOrRemove("CaseInsensitive", "original");
      assertThat(originalHeaders.getFirst("caseinsensitive"))
              .as("original mutated")
              .isEqualTo("original");
    }
    else {
      assertThatRuntimeException().isThrownBy(() -> headerMap.setOrRemove("CaseInsensitive", "original"));
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

  static Arguments initHeader(String description, MultiValueMap<String, String> headerMap) {
    headerMap.add("CaseInsensitive", "unmodified");
    return argumentSet(description, headerMap, true);
  }

  static Stream<Arguments> headers() {
    return Stream.of(
            initHeader("Map", MultiValueMap.forAdaption(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH))),
            initHeader("Netty", new Netty4HttpHeaders(new DefaultHttpHeaders())),
            //immutable versions of some headers
            argumentSet("Netty immutable", new Netty4HttpHeaders(new ReadOnlyHttpHeaders(false,
                    "CaseInsensitive", "unmodified")), false)
    );
  }

}