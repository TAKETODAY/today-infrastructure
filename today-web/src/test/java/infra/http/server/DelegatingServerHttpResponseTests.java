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
