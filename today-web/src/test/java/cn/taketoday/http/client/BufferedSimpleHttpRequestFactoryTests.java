/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Constant;

import static org.assertj.core.api.Assertions.assertThat;

public class BufferedSimpleHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new SimpleClientHttpRequestFactory();
  }

  @Override
  @Test
  public void httpMethods() throws Exception {
    try {
      assertHttpMethod("patch", HttpMethod.PATCH);
    }
    catch (ProtocolException ex) {
      // Currently HttpURLConnection does not support HTTP PATCH
    }
  }

  @Test
  public void prepareConnectionWithRequestBody() throws Exception {
    URL uri = new URL("https://example.com");
    testRequestBodyAllowed(uri, "GET", false);
    testRequestBodyAllowed(uri, "HEAD", false);
    testRequestBodyAllowed(uri, "OPTIONS", false);
    testRequestBodyAllowed(uri, "TRACE", false);
    testRequestBodyAllowed(uri, "PUT", true);
    testRequestBodyAllowed(uri, "POST", true);
    testRequestBodyAllowed(uri, "DELETE", true);
  }

  @Test
  public void deleteWithoutBodyDoesNotRaiseException() throws Exception {
    HttpURLConnection connection = new TestHttpURLConnection(new URL("https://example.com"));
    ((SimpleClientHttpRequestFactory) this.factory).prepareConnection(connection, HttpMethod.DELETE);
    SimpleBufferingClientHttpRequest request = new SimpleBufferingClientHttpRequest(connection, false);
    request.execute();
  }

  private void testRequestBodyAllowed(URL uri, String httpMethod, boolean allowed) throws IOException {
    HttpURLConnection connection = new TestHttpURLConnection(uri);
    ((SimpleClientHttpRequestFactory) this.factory).prepareConnection(
            connection, HttpMethod.valueOf(httpMethod));
    assertThat(connection.getDoOutput()).isEqualTo(allowed);
  }

  private static class TestHttpURLConnection extends HttpURLConnection {

    public TestHttpURLConnection(URL uri) {
      super(uri);
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(Constant.EMPTY_BYTES);
    }
  }

}
