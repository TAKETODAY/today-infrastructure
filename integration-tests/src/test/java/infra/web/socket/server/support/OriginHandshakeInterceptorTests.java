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

package infra.web.socket.server.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.web.socket.AbstractHttpRequestTests;
import infra.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link OriginHandshakeInterceptor}.
 *
 * @author Sebastien Deleuze
 */
public class OriginHandshakeInterceptorTests extends AbstractHttpRequestTests {

  private final Map<String, Object> attributes = new HashMap<>();
  private final WebSocketHandler wsHandler = mock(WebSocketHandler.class);

  @Test
  public void originValueMatch() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
    List<String> allowed = Collections.singletonList("https://mydomain1.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(mockResponse.getStatus());
  }

  @Test
  public void originValueNoMatch() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
    List<String> allowed = Collections.singletonList("https://mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(mockResponse.getStatus());
  }

  @Test
  public void originListMatch() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
    List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(mockResponse.getStatus());
  }

  @Test
  public void originListNoMatch() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
    List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(mockResponse.getStatus());
  }

  @Test
  public void originNoMatchWithNullHostileCollection() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
    interceptor.setAllowedOrigins(List.of("https://mydomain1.example"));
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(mockResponse.getStatus());
  }

  @Test
  public void originMatchAll() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
    interceptor.setAllowedOrigins(Collections.singletonList("*"));
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(mockResponse.getStatus());
  }

  @Test
  public void sameOriginMatchWithEmptyAllowedOrigins() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
    this.mockRequest.setServerName("mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(mockResponse.getStatus());
  }

  @Test
  public void sameOriginMatchWithAllowedOrigins() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
    this.mockRequest.setServerName("mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(List.of("http://mydomain1.example"));
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(mockResponse.getStatus());
  }

  @Test
  public void sameOriginNoMatch() throws Exception {
    this.mockRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain3.example");
    this.mockRequest.setServerName("mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(mockResponse.getStatus());
  }

  @Test
  void getAllowedOriginPatterns() {
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
    assertThat(interceptor.getAllowedOriginPatterns()).isEmpty();
    interceptor.setAllowedOriginPatterns(List.of("https://mydomain1.example"));
    assertThat(interceptor.getAllowedOriginPatterns()).hasSize(1).contains("https://mydomain1.example");
  }

  @Test
  void getAllowedOrigins() {
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
    assertThat(interceptor.getAllowedOrigins()).isEmpty();
    interceptor.setAllowedOrigins(List.of("https://mydomain1.example"));
    assertThat(interceptor.getAllowedOrigins()).hasSize(1).contains("https://mydomain1.example");
  }

}
