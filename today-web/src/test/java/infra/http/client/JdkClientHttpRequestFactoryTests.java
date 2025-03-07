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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 20:19
 */
class JdkClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Nullable
  private static String originalPropertyValue;

  @BeforeAll
  public static void setProperty() {
    originalPropertyValue = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
    System.setProperty("jdk.httpclient.allowRestrictedHeaders", "expect");
  }

  @AfterAll
  public static void restoreProperty() {
    if (originalPropertyValue != null) {
      System.setProperty("jdk.httpclient.allowRestrictedHeaders", originalPropertyValue);
    }
    else {
      System.clearProperty("jdk.httpclient.allowRestrictedHeaders");
    }
  }

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new JdkClientHttpRequestFactory();
  }

  @Override
  @Test
  public void httpMethods() throws Exception {
    super.httpMethods();
    assertHttpMethod("patch", HttpMethod.PATCH);
  }

  @Test
  public void customizeDisallowedHeaders() throws IOException {
    ClientHttpRequest request = this.factory.createRequest(URI.create(this.baseUrl + "/status/299"), HttpMethod.PUT);
    request.getHeaders().setOrRemove("Expect", "299");

    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatusCode.valueOf(299));
    }
  }

  @Test // gh-31451
  public void contentLength0() throws IOException {
    BufferingClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(this.factory);
    ClientHttpRequest request = bufferingFactory.createRequest(URI.create(this.baseUrl + "/methods/get"), HttpMethod.GET);

    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
    }
  }

}