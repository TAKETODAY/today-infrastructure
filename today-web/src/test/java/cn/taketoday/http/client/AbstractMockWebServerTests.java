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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;

import cn.taketoday.http.MediaType;
import cn.taketoday.util.StringUtils;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brian Clozel
 */
public abstract class AbstractMockWebServerTests {

  private MockWebServer server;

  protected int port;

  protected String baseUrl;

  protected static final MediaType textContentType =
          new MediaType("text", "plain", Collections.singletonMap("charset", "UTF-8"));

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
        return new MockResponse().setResponseCode(404);
      }
      catch (Throwable exc) {
        return new MockResponse().setResponseCode(500).setBody(exc.toString());
      }
    }
  }
}
