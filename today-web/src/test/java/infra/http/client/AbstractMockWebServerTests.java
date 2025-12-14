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
          assertThat(request.getHeader("Expect"))
                  .contains("299");
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
