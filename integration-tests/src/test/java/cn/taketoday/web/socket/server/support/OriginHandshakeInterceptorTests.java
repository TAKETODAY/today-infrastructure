/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.server.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.socket.AbstractHttpRequestTests;
import cn.taketoday.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
  public void invalidInput() {
    assertThatIllegalArgumentException().isThrownBy(() -> new OriginHandshakeInterceptor(null));
  }

  @Test
  public void originValueMatch() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
    List<String> allowed = Collections.singletonList("https://mydomain1.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
  }

  @Test
  public void originValueNoMatch() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
    List<String> allowed = Collections.singletonList("https://mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
  }

  @Test
  public void originListMatch() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
    List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
  }

  @Test
  public void originListNoMatch() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
    List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
  }

  @Test
  public void originNoMatchWithNullHostileCollection() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
    interceptor.setAllowedOrigins(List.of("https://mydomain1.example"));
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
  }

  @Test
  public void originMatchAll() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
    interceptor.setAllowedOrigins(Collections.singletonList("*"));
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
  }

  @Test
  public void sameOriginMatchWithEmptyAllowedOrigins() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
    this.servletRequest.setServerName("mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
  }

  @Test
  public void sameOriginMatchWithAllowedOrigins() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
    this.servletRequest.setServerName("mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(List.of("http://mydomain1.example"));
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isTrue();
    assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
  }

  @Test
  public void sameOriginNoMatch() throws Exception {
    this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain3.example");
    this.servletRequest.setServerName("mydomain2.example");
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
    assertThat(interceptor.beforeHandshake(request, wsHandler, attributes)).isFalse();
    assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
  }

}
