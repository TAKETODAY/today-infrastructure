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

package infra.http.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 00:34
 */
class DelegatingServerHttpResponseTests {

  private ServerHttpResponse mockDelegate;

  private DelegatingServerHttpResponse delegatingResponse;

  @BeforeEach
  void setUp() {
    mockDelegate = mock(ServerHttpResponse.class);
    delegatingResponse = new DelegatingServerHttpResponse(mockDelegate);
  }

  @Test
  void constructor_withNullDelegate_shouldThrowException() {
    assertThatThrownBy(() -> new DelegatingServerHttpResponse(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Delegate is required");
  }

  @Test
  void getDelegate_shouldReturnOriginalDelegate() {
    ServerHttpResponse delegate = delegatingResponse.getDelegate();
    assertThat(delegate).isSameAs(mockDelegate);
  }

  @Test
  void setStatusCode_shouldDelegateToTarget() {
    HttpStatusCode statusCode = HttpStatusCode.valueOf(200);

    delegatingResponse.setStatusCode(statusCode);

    verify(mockDelegate).setStatusCode(statusCode);
  }

  @Test
  void flush_shouldDelegateToTarget() throws IOException {
    delegatingResponse.flush();

    verify(mockDelegate).flush();
  }

  @Test
  void flush_withIOException_shouldPropagateException() throws IOException {
    doThrow(new IOException("Flush failed")).when(mockDelegate).flush();

    assertThatThrownBy(() -> delegatingResponse.flush())
            .isInstanceOf(IOException.class)
            .hasMessage("Flush failed");
  }

  @Test
  void close_shouldDelegateToTarget() {
    delegatingResponse.close();

    verify(mockDelegate).close();
  }

  @Test
  void getBody_shouldDelegateToTarget() throws IOException {
    OutputStream mockOutputStream = mock(OutputStream.class);
    when(mockDelegate.getBody()).thenReturn(mockOutputStream);

    OutputStream body = delegatingResponse.getBody();

    assertThat(body).isSameAs(mockOutputStream);
    verify(mockDelegate).getBody();
  }

  @Test
  void getBody_withIOException_shouldPropagateException() throws IOException {
    when(mockDelegate.getBody()).thenThrow(new IOException("Body unavailable"));

    assertThatThrownBy(() -> delegatingResponse.getBody())
            .isInstanceOf(IOException.class)
            .hasMessage("Body unavailable");
  }

  @Test
  void getHeaders_shouldDelegateToTarget() {
    HttpHeaders mockHeaders = HttpHeaders.forWritable();
    when(mockDelegate.getHeaders()).thenReturn(mockHeaders);

    HttpHeaders headers = delegatingResponse.getHeaders();

    assertThat(headers).isSameAs(mockHeaders);
    verify(mockDelegate).getHeaders();
  }

}
