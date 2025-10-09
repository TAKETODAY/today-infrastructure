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

package infra.http.client.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.http.HttpHeaders;
import infra.http.HttpRequest;
import infra.http.client.ClientHttpRequestExecution;
import infra.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 13:50
 */
class BasicAuthenticationInterceptorTests {

  @Test
  void constructorWithUsernameAndPassword() {
    BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor("user", "pass");
    assertThat(interceptor).isNotNull();
  }

  @Test
  void constructorWithUsernamePasswordAndCharset() {
    BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor("user", "pass", StandardCharsets.UTF_8);
    assertThat(interceptor).isNotNull();
  }

  @Test
  void interceptAddsBasicAuthHeaderWhenNotPresent() throws IOException {
    BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor("user", "pass");
    HttpRequest request = mock(HttpRequest.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(request.getHeaders()).thenReturn(headers);
    byte[] body = new byte[0];
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(execution.execute(request, body)).thenReturn(response);

    ClientHttpResponse result = interceptor.intercept(request, body, execution);

    assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isTrue();
    assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");
    assertThat(result).isSameAs(response);
  }

  @Test
  void interceptDoesNotOverrideExistingAuthHeader() throws IOException {
    BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor("user", "pass");
    HttpRequest request = mock(HttpRequest.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer token");
    when(request.getHeaders()).thenReturn(headers);
    byte[] body = new byte[0];
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(execution.execute(request, body)).thenReturn(response);

    ClientHttpResponse result = interceptor.intercept(request, body, execution);

    assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    assertThat(result).isSameAs(response);
  }

  @Test
  void interceptWithCustomCharset() throws IOException {
    BasicAuthenticationInterceptor interceptor = new BasicAuthenticationInterceptor("user", "pass", StandardCharsets.UTF_8);
    HttpRequest request = mock(HttpRequest.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(request.getHeaders()).thenReturn(headers);
    byte[] body = new byte[0];
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(execution.execute(request, body)).thenReturn(response);

    ClientHttpResponse result = interceptor.intercept(request, body, execution);

    assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isTrue();
    assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");
    assertThat(result).isSameAs(response);
  }


}