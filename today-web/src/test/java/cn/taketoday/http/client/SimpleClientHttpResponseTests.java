/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Brian Clozel
 * @author Juergen Hoeller
 */
public class SimpleClientHttpResponseTests {

  private final HttpURLConnection connection = mock();

  private final SimpleClientHttpResponse response = new SimpleClientHttpResponse(this.connection);

  @Test  // SPR-14040
  public void shouldNotCloseConnectionWhenResponseClosed() throws Exception {
    TestByteArrayInputStream is = new TestByteArrayInputStream("Spring".getBytes(StandardCharsets.UTF_8));
    given(this.connection.getResponseCode()).willReturn(200);
    given(this.connection.getInputStream()).willReturn(is);

    InputStream responseStream = this.response.getBody();
    assertThat(StreamUtils.copyToString(responseStream, StandardCharsets.UTF_8)).isEqualTo("Spring");

    this.response.close();
    assertThat(is.isClosed()).isTrue();
    verify(this.connection, never()).disconnect();
  }

  @Test  // SPR-14040
  public void shouldDrainStreamWhenResponseClosed() throws Exception {
    byte[] buf = new byte[6];
    TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(StandardCharsets.UTF_8));
    given(this.connection.getResponseCode()).willReturn(200);
    given(this.connection.getInputStream()).willReturn(is);

    InputStream responseStream = this.response.getBody();
    responseStream.read(buf);
    assertThat(new String(buf, StandardCharsets.UTF_8)).isEqualTo("Spring");
    assertThat(is.available()).isEqualTo(6);

    this.response.close();
    assertThat(is.available()).isEqualTo(0);
    assertThat(is.isClosed()).isTrue();
    verify(this.connection, never()).disconnect();
  }

  @Test  // SPR-14040
  public void shouldDrainErrorStreamWhenResponseClosed() throws Exception {
    byte[] buf = new byte[6];
    TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(StandardCharsets.UTF_8));
    given(this.connection.getResponseCode()).willReturn(404);
    given(this.connection.getErrorStream()).willReturn(is);

    InputStream responseStream = this.response.getBody();
    responseStream.read(buf);
    assertThat(new String(buf, StandardCharsets.UTF_8)).isEqualTo("Spring");
    assertThat(is.available()).isEqualTo(6);

    this.response.close();
    assertThat(is.available()).isEqualTo(0);
    assertThat(is.isClosed()).isTrue();
    verify(this.connection, never()).disconnect();
  }

  @Test  // SPR-16773
  public void shouldNotDrainWhenErrorStreamClosed() throws Exception {
    InputStream is = mock();
    given(this.connection.getResponseCode()).willReturn(404);
    given(this.connection.getErrorStream()).willReturn(is);
    willDoNothing().given(is).close();
    given(is.transferTo(any())).willCallRealMethod();
    given(is.read(any(), anyInt(), anyInt())).willThrow(new NullPointerException("from HttpURLConnection#ErrorStream"));

    is.readAllBytes();

    InputStream responseStream = this.response.getBody();
    responseStream.close();
    this.response.close();

    verify(is).close();
  }

  @Test // SPR-17181
  public void shouldDrainResponseEvenIfResponseNotRead() throws Exception {
    TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(StandardCharsets.UTF_8));
    given(this.connection.getResponseCode()).willReturn(200);
    given(this.connection.getInputStream()).willReturn(is);

    this.response.close();
    assertThat(is.available()).isEqualTo(0);
    assertThat(is.isClosed()).isTrue();
    verify(this.connection, never()).disconnect();
  }

  private static class TestByteArrayInputStream extends ByteArrayInputStream {

    private boolean closed;

    public TestByteArrayInputStream(byte[] buf) {
      super(buf);
      this.closed = false;
    }

    public boolean isClosed() {
      return closed;
    }

    @Override
    public void close() throws IOException {
      super.close();
      this.closed = true;
    }
  }

}
