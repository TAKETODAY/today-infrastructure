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

package infra.web.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ResponseStream}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ResponseStreamTests {

  @Test
  void inputStream_readsRawBytes() throws IOException {
    byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
    ResponseStream stream = stream(content);

    InputStream in = stream.inputStream();
    byte[] buf = new byte[32];
    int n = in.read(buf);
    assertThat(n).isEqualTo(11);
    assertThat(new String(buf, 0, n, StandardCharsets.UTF_8)).isEqualTo("hello world");

    stream.close();
  }

  @Test
  void lines_readsLineByLine() throws IOException {
    String content = "line1\nline2\nline3\n";
    ResponseStream stream = stream(content.getBytes(StandardCharsets.UTF_8));

    BufferedReader reader = stream.lines();
    assertThat(reader.readLine()).isEqualTo("line1");
    assertThat(reader.readLine()).isEqualTo("line2");
    assertThat(reader.readLine()).isEqualTo("line3");
    assertThat(reader.readLine()).isNull();

    stream.close();
  }

  @Test
  void sameReaderReturnedOnSubsequentCalls() throws IOException {
    ResponseStream stream = stream("data".getBytes(StandardCharsets.UTF_8));
    BufferedReader r1 = stream.lines();
    BufferedReader r2 = stream.lines();
    assertThat(r1).isSameAs(r2);
    stream.close();
  }

  @Test
  void close_releasesResponse() throws IOException {
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.getHeaders()).thenReturn(headers);

    ResponseStream stream = new ResponseStream(response);
    stream.close();

    // no exception expected; close() delegates to response.close()
  }

  @Test
  void headersReturnsResponseHeaders() throws IOException {
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.getHeaders()).thenReturn(headers);

    ResponseStream stream = new ResponseStream(response);
    assertThat(stream.headers()).isNotNull();
    stream.close();
  }

  @Test
  void statusCodeReturnsResponseStatus() throws IOException {
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.getHeaders()).thenReturn(headers);
    when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    when(response.getRawStatusCode()).thenReturn(200);

    ResponseStream stream = new ResponseStream(response);
    assertThat(stream.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(stream.rawStatusCode()).isEqualTo(200);
    stream.close();
  }

  private static ResponseStream stream(byte[] content) throws IOException {
    InputStream body = new ByteArrayInputStream(content);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.getHeaders()).thenReturn(headers);
    when(response.getBody()).thenReturn(body);
    return new ResponseStream(response);
  }
}
