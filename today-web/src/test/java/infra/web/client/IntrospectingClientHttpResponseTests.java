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

package infra.web.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.util.stream.Stream;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpResponse;
import infra.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link IntrospectingClientHttpResponse}.
 *
 * @author Yin-Jui Liao
 * @since 4.0
 */
class IntrospectingClientHttpResponseTests {

  @ParameterizedTest
  @MethodSource("noBodyHttpStatus")
  void noMessageBodyWhenStatus(HttpStatus status) throws Exception {
    var response = new MockClientHttpResponse(new byte[0], status);
    var wrapped = new IntrospectingClientHttpResponse(response);

    assertThat(wrapped.hasMessageBody()).isFalse();
  }

  static Stream<HttpStatusCode> noBodyHttpStatus() {
    return Stream.of(HttpStatus.NO_CONTENT, HttpStatus.EARLY_HINTS, HttpStatus.NOT_MODIFIED);
  }

  @Test
  void noMessageBodyWhenContentLength0() throws Exception {
    var response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
    response.getHeaders().setContentLength(0);
    var wrapped = new IntrospectingClientHttpResponse(response);

    assertThat(wrapped.hasMessageBody()).isFalse();
  }

  @Test
  void emptyMessageWhenNullInputStream() throws Exception {
    ClientHttpResponse mockResponse = mock();
    given(mockResponse.getBody()).willReturn(null);
    var wrappedMock = new IntrospectingClientHttpResponse(mockResponse);
    assertThat(wrappedMock.hasEmptyMessageBody()).isTrue();
  }

  @Test
  void messageBodyExists() throws Exception {
    var stream = new ByteArrayInputStream("content".getBytes());
    var response = new MockClientHttpResponse(stream, HttpStatus.OK);
    var wrapped = new IntrospectingClientHttpResponse(response);
    assertThat(wrapped.hasEmptyMessageBody()).isFalse();
  }

  @Test
  void emptyMessageWhenEOFException() throws Exception {
    ClientHttpResponse mockResponse = mock();
    InputStream stream = mock();
    given(mockResponse.getBody()).willReturn(stream);
    given(stream.read()).willThrow(new EOFException());
    var wrappedMock = new IntrospectingClientHttpResponse(mockResponse);
    assertThat(wrappedMock.hasEmptyMessageBody()).isTrue();
  }

}
