/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.util.FileCopyUtils;
import infra.util.StreamUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author Stephane Nicoll
 */
class HttpComponentsClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new HttpComponentsClientHttpRequestFactory();
  }

  @Override
  @Test
  void httpMethods() throws Exception {
    super.httpMethods();
    assertHttpMethod("patch", HttpMethod.PATCH);
  }

  @Test
  void shouldDecompressWithExpectedHeaders() throws Exception {
    ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/compress/gzip"), HttpMethod.POST);
    String message = "Hello World";
    final byte[] body = message.getBytes(StandardCharsets.UTF_8);
    StreamUtils.copy(body, request.getBody());

    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
      String result = FileCopyUtils.copyToString(new InputStreamReader(response.getBody()));
      assertThat(result).as("Invalid body").isEqualTo(message);
      assertThat(response.getHeaders().get(HttpHeaders.CONTENT_ENCODING)).isNull();
      assertThat(response.getHeaders().getContentLength()).isEqualTo(-1);
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  void assertCustomConfig() {
    HttpClient httpClient = HttpClientBuilder.create().build();
    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(httpClient);
    hrf.setConnectTimeout(1234);
    hrf.setConnectionRequestTimeout(4321);

    URI uri = URI.create(baseUrl + "/status/ok");
    HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest) hrf.createRequest(uri, HttpMethod.GET);

    Object config = request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
    assertThat(config).as("Request config should be set").isNotNull();
    assertThat(config).as("Wrong request config type " + config.getClass().getName()).isInstanceOf(RequestConfig.class);
    RequestConfig requestConfig = (RequestConfig) config;
    assertThat(requestConfig.getConnectTimeout()).as("Wrong custom connection timeout").isEqualTo(Timeout.of(1234, MILLISECONDS));
    assertThat(requestConfig.getConnectionRequestTimeout()).as("Wrong custom connection request timeout").isEqualTo(Timeout.of(4321, MILLISECONDS));
  }

  @Test
  @SuppressWarnings("deprecation")
  void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws Exception {
    @SuppressWarnings("deprecation")
    RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234, MILLISECONDS).build();
    CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
    assertThat(retrieveRequestConfig(hrf)).as("Default client configuration is expected").isSameAs(defaultConfig);

    hrf.setConnectionRequestTimeout(4567);
    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig).isNotNull();
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(4567, MILLISECONDS));
    // Default connection timeout merged
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.of(1234, MILLISECONDS));
  }

  @Test
  @SuppressWarnings("deprecation")
  void localSettingsOverrideClientDefaultSettings() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom()
            .setConnectTimeout(1234, MILLISECONDS)
            .setConnectionRequestTimeout(6789, MILLISECONDS)
            .build();
    CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
    hrf.setConnectTimeout(5000);

    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.of(5000, MILLISECONDS));
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(6789, MILLISECONDS));
  }

  @Test
  @SuppressWarnings("deprecation")
  void mergeBasedOnCurrentHttpClient() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(1234, MILLISECONDS)
            .build();
    CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory() {
      @Override
      public HttpClient getHttpClient() {
        return client;
      }
    };
    hrf.setConnectionRequestTimeout(5000);

    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(5000, MILLISECONDS));
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(RequestConfig.DEFAULT.getConnectTimeout());

    // Update the Http client so that it returns an updated config
    RequestConfig updatedDefaultConfig = RequestConfig.custom()
            .setConnectTimeout(1234, MILLISECONDS).build();
    given(configurable.getConfig()).willReturn(updatedDefaultConfig);
    hrf.setConnectionRequestTimeout(7000);
    RequestConfig requestConfig2 = retrieveRequestConfig(hrf);
    assertThat(requestConfig2.getConnectTimeout()).isEqualTo(Timeout.of(1234, MILLISECONDS));
    assertThat(requestConfig2.getConnectionRequestTimeout()).isEqualTo(Timeout.of(7000, MILLISECONDS));
  }

  @ParameterizedTest
  @MethodSource("unsafeHttpMethods")
  void shouldSetContentLengthWhenEmptyBody(HttpMethod method) throws Exception {
    ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/header/Content-Length"), method);
    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
      String result = FileCopyUtils.copyToString(new InputStreamReader(response.getBody()));
      assertThat(result).as("Invalid body").isEqualTo("Content-Length:0");
    }
  }

  static Stream<HttpMethod> unsafeHttpMethods() {
    return Stream.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH);
  }

  @ParameterizedTest
  @MethodSource("safeHttpMethods")
  void shouldNotSetContentLengthWhenEmptyBodyAndSafeMethod(HttpMethod method) throws Exception {
    ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/header/Content-Length"), method);
    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
      String result = FileCopyUtils.copyToString(new InputStreamReader(response.getBody()));
      assertThat(result).as("Invalid body").isEqualTo("Content-Length:null");
    }
  }

  static Stream<HttpMethod> safeHttpMethods() {
    return Stream.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.TRACE);
  }

  @SuppressWarnings("deprecation")
  private RequestConfig retrieveRequestConfig(HttpComponentsClientHttpRequestFactory factory) {
    URI uri = URI.create(baseUrl + "/status/ok");
    HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
            factory.createRequest(uri, HttpMethod.GET);
    return (RequestConfig) request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
  }

  @Test
  void constructorWithDefaultHttpClient() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThat(factory.getHttpClient()).isNotNull();
  }

  @Test
  void constructorWithCustomHttpClient() {
    HttpClient httpClient = mock(HttpClient.class);
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
    assertThat(factory.getHttpClient()).isSameAs(httpClient);
  }

  @Test
  void setHttpClientWithValidClient() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    HttpClient httpClient = mock(HttpClient.class);
    factory.setHttpClient(httpClient);
    assertThat(factory.getHttpClient()).isSameAs(httpClient);
  }

  @Test
  void setHttpClientWithNullThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setHttpClient(null))
            .withMessage("HttpClient is required");
  }

  @Test
  void setConnectTimeoutWithValidValue() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    // Should not throw exception
  }

  @Test
  void setConnectTimeoutWithZeroValue() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(0);
    // Should not throw exception
  }

  @Test
  void setConnectTimeoutWithNegativeValueThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectTimeout(-1))
            .withMessage("Timeout must be a non-negative value");
  }

  @Test
  void setConnectTimeoutWithDuration() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    // Should not throw exception
  }

  @Test
  void setConnectTimeoutWithNullDurationThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectTimeout((Duration) null))
            .withMessage("ConnectTimeout is required");
  }

  @Test
  void setConnectTimeoutWithDurationNegativeThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectTimeout(Duration.ofMillis(-1)))
            .withMessage("Timeout must be a non-negative value");
  }

  @Test
  void setConnectionRequestTimeoutWithValidValue() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectionRequestTimeout(5000);
    // Should not throw exception
  }

  @Test
  void setConnectionRequestTimeoutWithZeroValue() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectionRequestTimeout(0);
    // Should not throw exception
  }

  @Test
  void setConnectionRequestTimeoutWithNegativeValueThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectionRequestTimeout(-1))
            .withMessage("Timeout must be a non-negative value");
  }

  @Test
  void setConnectionRequestTimeoutWithDuration() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
    // Should not throw exception
  }

  @Test
  void setConnectionRequestTimeoutWithNullDurationThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectionRequestTimeout((Duration) null))
            .withMessage("ConnectionRequestTimeout is required");
  }

  @Test
  void setConnectionRequestTimeoutWithDurationNegativeThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectionRequestTimeout(Duration.ofMillis(-1)))
            .withMessage("Timeout must be a non-negative value");
  }

  @Test
  void setReadTimeoutWithValidValue() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setReadTimeout(5000);
    // Should not throw exception
  }

  @Test
  void setReadTimeoutWithZeroValue() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setReadTimeout(0);
    // Should not throw exception
  }

  @Test
  void setReadTimeoutWithNegativeValueThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setReadTimeout(-1))
            .withMessage("Timeout must be a non-negative value");
  }

  @Test
  void setReadTimeoutWithDuration() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(5));
    // Should not throw exception
  }

  @Test
  void setReadTimeoutWithNullDurationThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setReadTimeout((Duration) null))
            .withMessage("ReadTimeout is required");
  }

  @Test
  void setReadTimeoutWithDurationNegativeThrowsException() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setReadTimeout(Duration.ofMillis(-1)))
            .withMessage("Timeout must be a non-negative value");
  }

  @Test
  void setHttpContextFactory() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    BiFunction<HttpMethod, URI, HttpContext> contextFactory = (method, uri) -> HttpClientContext.create();
    factory.setHttpContextFactory(contextFactory);
    // Should not throw exception
  }

  @Test
  void createHttpUriRequestForAllSupportedMethods() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    URI uri = URI.create("http://example.com");

    assertThat(factory.createHttpUriRequest(HttpMethod.GET, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpGet.class);
    assertThat(factory.createHttpUriRequest(HttpMethod.POST, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpPost.class);
    assertThat(factory.createHttpUriRequest(HttpMethod.PUT, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpPut.class);
    assertThat(factory.createHttpUriRequest(HttpMethod.DELETE, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpDelete.class);
    assertThat(factory.createHttpUriRequest(HttpMethod.HEAD, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpHead.class);
    assertThat(factory.createHttpUriRequest(HttpMethod.OPTIONS, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpOptions.class);
    assertThat(factory.createHttpUriRequest(HttpMethod.TRACE, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpTrace.class);
    assertThat(factory.createHttpUriRequest(HttpMethod.PATCH, uri)).isInstanceOf(org.apache.hc.client5.http.classic.methods.HttpPatch.class);
  }

  @Test
  void createHttpUriRequestForUnsupportedMethod() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    URI uri = URI.create("http://example.com");

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> factory.createHttpUriRequest(HttpMethod.CONNECT, uri))
            .withMessageContaining("Unsupported httpMethod 'CONNECT'");
  }

  @Test
  void mergeRequestConfigWithNoTimeouts() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    RequestConfig config = RequestConfig.DEFAULT;

    RequestConfig mergedConfig = factory.mergeRequestConfig(config);
    assertThat(mergedConfig).isSameAs(config);
  }

  @Test
  void mergeRequestConfigWithCustomTimeouts() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(1000);
    factory.setConnectionRequestTimeout(2000);
    factory.setReadTimeout(3000);

    RequestConfig config = RequestConfig.DEFAULT;
    RequestConfig mergedConfig = factory.mergeRequestConfig(config);

    assertThat(mergedConfig).isNotNull();
    assertThat(mergedConfig).isNotSameAs(config);
  }

  @Test
  void createRequestConfigWithNonConfigurableClient() {
    HttpClient httpClient = mock(HttpClient.class);
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

    RequestConfig config = factory.createRequestConfig(httpClient);
    assertThat(config).isSameAs(RequestConfig.DEFAULT);
  }

  @Test
  void destroyWithCloseableHttpClient() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

    factory.destroy();
    // Should not throw exception
  }

}
