/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.server.reactive;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;

import java.util.Locale;
import java.util.stream.Stream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.support.Netty4HttpHeaders;
import cn.taketoday.http.support.Netty5HttpHeaders;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpRequest;
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
            initHeader("Netty5", new Netty5HttpHeaders(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders())),
            //immutable versions of some headers
            argumentSet("Netty immutable", new Netty4HttpHeaders(new ReadOnlyHttpHeaders(false,
                    "CaseInsensitive", "unmodified")), false)
    );
  }

}