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

package infra.web.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import infra.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ClientResponseTests {

  @Test
  void readsStringWithNullCharsetAsUtf8() throws IOException {
    ClientResponse response = response(new ByteArrayInputStream("你好".getBytes(StandardCharsets.UTF_8)));

    assertThat(response.getContentAsString(null)).isEqualTo("你好");
  }

  @Test
  void readsRemainingBytes() throws IOException {
    byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
    var body = new ByteArrayInputStream(bytes);
    body.read();
    ClientResponse response = response(body);

    assertThat(response.getContentAsByteArray()).containsExactly("ello".getBytes(StandardCharsets.UTF_8));
    assertThat(response.getContentAsByteArray()).isEmpty();
  }

  @Test
  void readsUtf8String() throws IOException {
    ClientResponse response = response(new ByteArrayInputStream("你好".getBytes(StandardCharsets.UTF_8)));

    assertThat(response.getContentAsString()).isEqualTo("你好");
  }

  @Test
  void readsStringWithExplicitCharset() throws IOException {
    ClientResponse response = response(new ByteArrayInputStream("你好".getBytes(StandardCharsets.UTF_16LE)));

    assertThat(response.getContentAsString(StandardCharsets.UTF_16LE)).isEqualTo("你好");
  }

  @ParameterizedTest
  @ValueSource(strings = { "bytes", "string", "charset" })
  void consumesBodyWithoutClosing(String method) throws IOException {
    InputStream body = spy(new ByteArrayInputStream(new byte[] { 65 }));
    ClientResponse response = response(body);

    read(response, method);

    assertThat(body.read()).isEqualTo(-1);
    verify(body, never()).close();
    verify(response, never()).close();
  }

  @Test
  void readsEmptyBody() throws IOException {
    ClientResponse response = response(InputStream.nullInputStream());

    assertThat(response.getContentAsByteArray()).isEmpty();
    assertThat(response.getContentAsString()).isEmpty();
    assertThat(response.getContentAsString(StandardCharsets.UTF_16LE)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = { "bytes", "string", "charset" })
  void wrapsReadFailureWithoutClosing(String method) throws IOException {
    InputStream body = mock(InputStream.class);
    IOException failure = new IOException("read failure");
    given(body.readAllBytes()).willThrow(failure);
    given(body.read(any(byte[].class), anyInt(), anyInt())).willThrow(failure);
    ClientResponse response = response(body);

    assertThatExceptionOfType(RestClientException.class)
            .isThrownBy(() -> read(response, method))
            .withCause(failure);
    verify(body, never()).close();
    verify(response, never()).close();
  }

  @ParameterizedTest
  @ValueSource(strings = { "bytes", "string", "charset" })
  void wrapsBodyAccessFailureWithoutClosing(String method) throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class, CALLS_REAL_METHODS);
    IOException failure = new IOException("body access failure");
    given(delegate.getBody()).willThrow(failure);
    ClientResponse response = response(delegate);

    assertThatExceptionOfType(RestClientException.class)
            .isThrownBy(() -> read(response, method))
            .withCause(failure);
    verify(response, never()).close();
  }

  private static ClientResponse response(InputStream body) throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class, CALLS_REAL_METHODS);
    given(delegate.getBody()).willReturn(body);
    return response(delegate);
  }

  private static ClientResponse response(ClientHttpResponse delegate) {
    DefaultRestClient client = (DefaultRestClient) RestClient.builder().build();
    return spy(client.new DefaultClientResponse(delegate, null));
  }

  private static Object read(ClientResponse response, String method) {
    return switch (method) {
      case "bytes" -> response.getContentAsByteArray();
      case "string" -> response.getContentAsString();
      case "charset" -> response.getContentAsString(StandardCharsets.UTF_16LE);
      default -> throw new IllegalArgumentException(method);
    };
  }

}
