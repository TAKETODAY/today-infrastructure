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