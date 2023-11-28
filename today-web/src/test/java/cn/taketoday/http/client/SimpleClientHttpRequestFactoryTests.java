/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class SimpleClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new SimpleClientHttpRequestFactory();
  }

  @Override
  @Test
  public void httpMethods() throws Exception {
    super.httpMethods();
    assertThatExceptionOfType(ProtocolException.class).isThrownBy(() ->
            assertHttpMethod("patch", HttpMethod.PATCH));
  }

  @Test
  public void prepareConnectionWithRequestBody() throws Exception {
    URI uri = new URI("https://example.com");
    testRequestBodyAllowed(uri, "GET", false);
    testRequestBodyAllowed(uri, "HEAD", false);
    testRequestBodyAllowed(uri, "OPTIONS", false);
    testRequestBodyAllowed(uri, "TRACE", false);
    testRequestBodyAllowed(uri, "PUT", true);
    testRequestBodyAllowed(uri, "POST", true);
    testRequestBodyAllowed(uri, "DELETE", true);
  }

  private void testRequestBodyAllowed(URI uri, String httpMethod, boolean allowed) throws IOException {
    HttpURLConnection connection = new TestHttpURLConnection(uri.toURL());
    ((SimpleClientHttpRequestFactory) this.factory).prepareConnection(connection, HttpMethod.valueOf(httpMethod));
    assertThat(connection.getDoOutput()).isEqualTo(allowed);
  }

  @Test
  public void deleteWithoutBodyDoesNotRaiseException() throws Exception {
    HttpURLConnection connection = new TestHttpURLConnection(new URL("https://example.com"));
    ((SimpleClientHttpRequestFactory) this.factory).prepareConnection(connection, HttpMethod.DELETE);
    SimpleClientHttpRequest request = new SimpleClientHttpRequest(connection, 4096);
    request.execute();
  }

  @Test  // SPR-8809
  public void interceptor() throws Exception {
    final String headerName = "MyHeader";
    final String headerValue = "MyValue";
    ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
      request.getHeaders().add(headerName, headerValue);
      return execution.execute(request, body);
    };
    InterceptingClientHttpRequestFactory factory = new InterceptingClientHttpRequestFactory(
            createRequestFactory(), Collections.singletonList(interceptor));

    ClientHttpResponse response = null;
    try {
      ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/echo"), HttpMethod.GET);
      response = request.execute();
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
      HttpHeaders responseHeaders = response.getHeaders();
      assertThat(responseHeaders.getFirst(headerName)).as("Custom header invalid").isEqualTo(headerValue);
    }
    finally {
      if (response != null) {
        response.close();
      }
    }
  }

  @Test // SPR-13225
  public void headerWithNullValue() {
    HttpURLConnection urlConnection = mock();
    given(urlConnection.getRequestMethod()).willReturn("GET");
    HttpHeaders headers = HttpHeaders.create();
    headers.set("foo", null);
    SimpleClientHttpRequest.addHeaders(urlConnection, headers);
    verify(urlConnection, times(1)).addRequestProperty("foo", "");
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
      return new ByteArrayInputStream(new byte[0]);
    }
  }

}

