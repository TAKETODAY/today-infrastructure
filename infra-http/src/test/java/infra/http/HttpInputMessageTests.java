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

package infra.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class HttpInputMessageTests {

  @Test
  void readsStringWithNullCharsetAsUtf8() throws IOException {
    HttpInputMessage message = new MockHttpInputMessage("你好".getBytes(StandardCharsets.UTF_8));
    message.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_16LE));

    assertThat(message.getContentAsString(null)).isEqualTo("你好");
  }

  @Test
  void readsRemainingBytes() throws IOException {
    InputStream body = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
    body.read();
    HttpInputMessage message = new MockHttpInputMessage(body);

    assertThat(message.getContentAsByteArray()).containsExactly(2, 3);
    assertThat(message.getContentAsByteArray()).isEmpty();
  }

  @Test
  void readsStringsWithDefaultAndExplicitCharset() throws IOException {
    HttpInputMessage utf8 = new MockHttpInputMessage("你好".getBytes(StandardCharsets.UTF_8));
    utf8.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_16LE));
    HttpInputMessage utf16 = new MockHttpInputMessage("你好".getBytes(StandardCharsets.UTF_16LE));
    utf16.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

    assertThat(utf8.getContentAsString()).isEqualTo("你好");
    assertThat(utf16.getContentAsString(StandardCharsets.UTF_16LE)).isEqualTo("你好");
  }

  @ParameterizedTest
  @ValueSource(strings = { "bytes", "string", "charset" })
  void readsEmptyBody(String method) throws IOException {
    Object result = read(new MockHttpInputMessage(new byte[0]), method);

    if (result instanceof byte[] bytes) {
      assertThat(bytes).isEmpty();
    }
    else {
      assertThat(result).isEqualTo("");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "bytes", "string", "charset" })
  void consumesBodyWithoutClosing(String method) throws IOException {
    InputStream body = spy(new ByteArrayInputStream(new byte[] { 65 }));

    read(new MockHttpInputMessage(body), method);

    assertThat(body.read()).isEqualTo(-1);
    verify(body, never()).close();
  }

  @ParameterizedTest
  @ValueSource(strings = { "bytes", "string", "charset" })
  void propagatesReadFailureWithoutClosing(String method) throws IOException {
    InputStream body = mock(InputStream.class);
    IOException failure = new IOException("read failure");
    given(body.readAllBytes()).willThrow(failure);
    given(body.read(any(byte[].class), anyInt(), anyInt())).willThrow(failure);

    assertThatIOException().isThrownBy(() -> read(new MockHttpInputMessage(body), method))
            .isSameAs(failure);
    verify(body, never()).close();
  }

  @ParameterizedTest
  @ValueSource(strings = { "bytes", "string", "charset" })
  void propagatesBodyAccessFailure(String method) throws IOException {
    HttpInputMessage message = mock(HttpInputMessage.class, CALLS_REAL_METHODS);
    IOException failure = new IOException("body access failure");
    given(message.getBody()).willThrow(failure);

    assertThatIOException().isThrownBy(() -> read(message, method)).isSameAs(failure);
  }

  private static Object read(HttpInputMessage message, String method) throws IOException {
    return switch (method) {
      case "bytes" -> message.getContentAsByteArray();
      case "string" -> message.getContentAsString();
      case "charset" -> message.getContentAsString(StandardCharsets.UTF_16LE);
      default -> throw new IllegalArgumentException(method);
    };
  }

}
