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

package infra.web.util;

import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import infra.http.MockHttpInputMessage;
import infra.http.converter.HttpMessageNotReadableException;
import infra.web.client.ResourceAccessException;
import reactor.netty.channel.AbortedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/31 22:13
 */
class DisconnectedClientHelperTests {

  @ParameterizedTest
  @ValueSource(strings = { "broKen pipe", "connection reset By peer" })
  void exceptionPhrases(String phrase) {
    Exception ex = new IOException(phrase);
    assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();

    ex = new IOException(ex);
    assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();
  }

  @Test
  void connectionResetExcluded() {
    Exception ex = new IOException("connection reset");
    assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isFalse();
  }

  @ParameterizedTest
  @MethodSource("disconnectedExceptions")
  void name(Exception ex) {
    assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();
  }

  static List<Exception> disconnectedExceptions() {
    return List.of(
            new AbortedException(""),
            new EOFException(), new EofException());
  }

  @Test
  void nestedDisconnectedException() {
    Exception ex = new HttpMessageNotReadableException(
            "I/O error while reading input message", new EofException(),
            new MockHttpInputMessage(new byte[0]));

    assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isTrue();
  }

  @Test
  void onwardClientDisconnectedExceptionPhrase() {
    Exception ex = new ResourceAccessException("I/O error", new EOFException("Connection reset by peer"));
    assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isFalse();
  }

  @Test
  void onwardClientDisconnectedExceptionType() {
    Exception ex = new ResourceAccessException("I/O error", new EOFException());
    assertThat(DisconnectedClientHelper.isClientDisconnectedException(ex)).isFalse();
  }

  @Test
  void nullException() {
    assertThat(DisconnectedClientHelper.isClientDisconnectedException(null)).isFalse();
  }
}