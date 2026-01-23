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

package infra.http.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import infra.http.HttpHeaders;
import infra.util.StringUtils;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brian Clozel
 */
public abstract class AbstractMockWebServerTests {

  private MockWebServer server;

  protected int port;

  protected String baseUrl;

  @BeforeEach
  public void setUp() throws Exception {
    this.server = new MockWebServer();
    this.server.setDispatcher(new TestDispatcher());
    this.server.start();
    this.port = this.server.getPort();
    this.baseUrl = "http://localhost:" + this.port;
  }

  @AfterEach
  public void tearDown() throws Exception {
    this.server.shutdown();
  }

  protected class TestDispatcher extends Dispatcher {
    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
      try {
        if (request.getPath().equals("/echo")) {
          assertThat(request.getHeader("Host"))
                  .contains("localhost:" + port);
          MockResponse response = new MockResponse()
                  .setHeaders(request.getHeaders())
                  .setHeader("Content-Length", request.getBody().size())
                  .setResponseCode(200)
                  .setBody(request.getBody());
          request.getBody().flush();
          return response;
        }
        else if (request.getPath().equals("/status/ok")) {
          return new MockResponse();
        }
        else if (request.getPath().equals("/status/notfound")) {
          return new MockResponse().setResponseCode(404);
        }
        else if (request.getPath().equals("/status/299")) {
          assertThat(request.getHeader("Expect")).contains("299");
          return new MockResponse().setResponseCode(299);
        }
        else if (request.getPath().startsWith("/params")) {
          assertThat(request.getPath()).contains("param1=value");
          assertThat(request.getPath()).contains("param2=value1&param2=value2");
          return new MockResponse();
        }
        else if (request.getPath().equals("/methods/post")) {
          assertThat(request.getMethod()).isEqualTo("POST");
          String transferEncoding = request.getHeader("Transfer-Encoding");
          if (StringUtils.isNotEmpty(transferEncoding)) {
            assertThat(transferEncoding).isEqualTo("chunked");
          }
          else {
            long contentLength = Long.parseLong(request.getHeader("Content-Length"));
            assertThat(request.getBody().size()).isEqualTo(contentLength);
          }
          return new MockResponse().setResponseCode(200);
        }
        else if (request.getPath().startsWith("/methods/")) {
          String expectedMethod = request.getPath().replace("/methods/", "").toUpperCase();
          assertThat(request.getMethod()).isEqualTo(expectedMethod);
          return new MockResponse();
        }
        else if (request.getPath().startsWith("/header/")) {
          String headerName = request.getPath().replace("/header/", "");
          return new MockResponse().setBody(headerName + ":" + request.getHeader(headerName)).setResponseCode(200);
        }
        else if (request.getMethod().equals("POST") && request.getPath().startsWith("/compress/") && request.getBody() != null) {
          String encoding = request.getPath().replace("/compress/", "");
          String requestBody = request.getBody().readUtf8();
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          if (encoding.equals("deflate")) {
            try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
              deflaterOutputStream.write(requestBody.getBytes());
              deflaterOutputStream.flush();
            }
          }
          // compress anyway with gzip
          else {
            encoding = "gzip";
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
              gzipOutputStream.write(requestBody.getBytes());
              gzipOutputStream.flush();
            }
          }
          Buffer buffer = new Buffer();
          buffer.write(outputStream.toByteArray());
          MockResponse response = new MockResponse();
          response.setBody(buffer);
          response.setResponseCode(200);
          response.setHeader(HttpHeaders.CONTENT_ENCODING, encoding);
          return response;
        }
        else if (request.getMethod().equals("HEAD") && request.getPath().startsWith("/headforcompress/")) {
          String encoding = request.getPath().replace("/headforcompress/", "");
          MockResponse response = new MockResponse();
          response.setHeader(HttpHeaders.CONTENT_LENGTH, 500);
          response.setHeader(HttpHeaders.CONTENT_ENCODING, encoding);
          response.setResponseCode(200);
          return response;
        }
        return new MockResponse().setResponseCode(404);
      }
      catch (Throwable exc) {
        return new MockResponse().setResponseCode(500).setBody(exc.toString());
      }
    }
  }
}
