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

package infra.web.util;

import org.apache.catalina.connector.ClientAbortException;
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
            new AbortedException(""), new ClientAbortException(""),
            new EOFException(), new EofException());
  }

  @Test
  void nestedDisconnectedException() {
    Exception ex = new HttpMessageNotReadableException(
            "I/O error while reading input message", new ClientAbortException(),
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