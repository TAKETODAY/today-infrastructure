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

package infra.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.util.MultiValueMap;
import infra.web.async.AsyncWebRequest;
import infra.web.multipart.MultipartRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestContext#getRequestURL()}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class RequestContextRequestUrlTests {

  private static StubRequestContext create(String scheme, String serverName, int port, String requestURI) {
    return new StubRequestContext(scheme, serverName, port, requestURI);
  }

  @Test
  void httpDefaultPortOmitsPort() {
    var ctx = create("http", "example.com", 80, "/path");
    assertThat(ctx.getRequestURL()).isEqualTo("http://example.com/path");
  }

  @Test
  void httpNonDefaultPortIncludesPort() {
    var ctx = create("http", "example.com", 8080, "/path");
    assertThat(ctx.getRequestURL()).isEqualTo("http://example.com:8080/path");
  }

  @Test
  void httpsDefaultPortOmitsPort() {
    var ctx = create("https", "example.com", 443, "/path");
    assertThat(ctx.getRequestURL()).isEqualTo("https://example.com/path");
  }

  @Test
  void httpsNonDefaultPortIncludesPort() {
    var ctx = create("https", "example.com", 8443, "/secure");
    assertThat(ctx.getRequestURL()).isEqualTo("https://example.com:8443/secure");
  }

  @Test
  void prependLeadingSlash() {
    var ctx = create("http", "example.com", 80, "no-slash");
    assertThat(ctx.getRequestURL()).isEqualTo("http://example.com/no-slash");
  }

  @Test
  void customSchemeWithPort() {
    var ctx = create("ftp", "files.example.com", 21, "/data");
    assertThat(ctx.getRequestURL()).isEqualTo("ftp://files.example.com:21/data");
  }

  // -- stub --

  private static class StubRequestContext extends RequestContext {

    private final String scheme;

    private final String serverName;

    private final int serverPort;

    private final String requestURI;

    StubRequestContext(String scheme, String serverName, int serverPort, String requestURI) {
      super(null, null);
      this.scheme = scheme;
      this.serverName = serverName;
      this.serverPort = serverPort;
      this.requestURI = requestURI;
    }

    @Override
    public String getScheme() {
      return scheme;
    }

    @Override
    public String getServerName() {
      return serverName;
    }

    @Override
    public int getServerPort() {
      return serverPort;
    }

    @Override
    protected String readRequestURI() {
      return requestURI;
    }

    // --- remaining abstract methods: minimal stubs ---

    @Override
    public long getRequestTimeMillis() {
      return 0;
    }

    @Override
    public boolean isSecure() {
      return false;
    }

    @Override
    public String getRemoteAddress() {
      return null;
    }

    @Override
    public SocketAddress localAddress() {
      return null;
    }

    @Override
    public InetSocketAddress remoteAddress() {
      return null;
    }

    @Override
    protected String readQueryString() {
      return null;
    }

    @Override
    protected HttpCookie[] readCookies() {
      return new HttpCookie[0];
    }

    @Override
    protected MultiValueMap<String, String> readParameters() {
      return MultiValueMap.forLinkedHashMap();
    }

    @Override
    protected String readMethod() {
      return "GET";
    }

    @Override
    public long getContentLength() {
      return -1;
    }

    @Override
    protected InputStream createInputStream() {
      return null;
    }

    @Override
    protected MultipartRequest createMultipartRequest() {
      return null;
    }

    @Override
    protected AsyncWebRequest createAsyncWebRequest() {
      return null;
    }

    @Override
    public String getContentTypeAsString() {
      return null;
    }

    @Override
    protected HttpHeaders createRequestHeaders() {
      return HttpHeaders.forWritable();
    }

    @Override
    public boolean isCommitted() {
      return false;
    }

    @Override
    public void sendRedirect(String location) {
    }

    @Override
    public void setStatus(int sc) {
    }

    @Override
    public int getStatus() {
      return 200;
    }

    @Override
    public void sendError(int sc) {
    }

    @Override
    public void sendError(int sc, String msg) {
    }

    @Override
    protected OutputStream createOutputStream() {
      return null;
    }

    @Override
    public <T> T nativeRequest() {
      return null;
    }
  }
}
