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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.util.StreamUtils;

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

  @Test
  public void contentLength0() throws IOException {
    BufferingClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(this.factory);
    ClientHttpRequest request = bufferingFactory.createRequest(URI.create(this.baseUrl + "/methods/get"), HttpMethod.GET);

    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
    }
  }

  @Test
  void compressionDisabled() throws IOException {
    URI uri = URI.create(baseUrl + "/compress/");
    if (this.factory instanceof JdkClientHttpRequestFactory jdkClientHttpRequestFactory) {
      jdkClientHttpRequestFactory.enableCompression(false);
    }
    ClientHttpRequest request = this.factory.createRequest(uri, HttpMethod.POST);
    StreamUtils.copy("Payload to compress", StandardCharsets.UTF_8, request.getBody());
    try (ClientHttpResponse response = request.execute()) {
      assertThat(request.getHeaders().containsKey("Accept-Encoding")).isFalse();
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().containsKey("Content-Encoding")).isTrue();
      assertThat(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8))
              .as("Body should not be decompressed")
              .doesNotContain("Payload to compress");
    }
  }

  @Test
  void compressionGzip() throws IOException {
    URI uri = URI.create(baseUrl + "/compress/gzip");
    JdkClientHttpRequestFactory requestFactory = (JdkClientHttpRequestFactory) this.factory;
    requestFactory.enableCompression(true);
    ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
    StreamUtils.copy("Payload to compress", StandardCharsets.UTF_8, request.getBody());
    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getFirst("Content-Encoding"))
              .as("Invalid content encoding").isEqualTo("gzip");
      assertThat(response.getBody()).as("Invalid request body").hasContent("Payload to compress");
    }
  }

  @Test
  void compressionDeflate() throws IOException {
    URI uri = URI.create(baseUrl + "/compress/deflate");
    JdkClientHttpRequestFactory requestFactory = (JdkClientHttpRequestFactory) this.factory;
    requestFactory.enableCompression(true);
    ClientHttpRequest request = requestFactory.createRequest(uri, HttpMethod.POST);
    StreamUtils.copy("Payload to compress", StandardCharsets.UTF_8, request.getBody());
    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getFirst("Content-Encoding"))
              .as("Invalid content encoding").isEqualTo("deflate");
      assertThat(response.getBody()).as("Invalid request body").hasContent("Payload to compress");
    }
  }

}