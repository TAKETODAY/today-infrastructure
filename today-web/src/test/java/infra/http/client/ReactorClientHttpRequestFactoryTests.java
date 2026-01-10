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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

import infra.http.HttpMethod;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/14 16:33
 */
class ReactorClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new ReactorClientHttpRequestFactory();
  }

  @Override
  @Test
  void httpMethods() throws Exception {
    super.httpMethods();
    assertHttpMethod("patch", HttpMethod.PATCH);
  }

  @Test
  void restartWithDefaultConstructor() {
    ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.stop();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
  }

  @Test
  void restartWithHttpClient() {
    HttpClient httpClient = HttpClient.create();
    ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.stop();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
  }

  @Test
  void restartWithExternalResourceFactory() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    resourceFactory.afterPropertiesSet();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.stop();
    assertThat(requestFactory.isRunning()).isFalse();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
  }

  @Test
  void lateStartWithExternalResourceFactory() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
    assertThat(requestFactory.isRunning()).isFalse();
    resourceFactory.start();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.stop();
    assertThat(requestFactory.isRunning()).isFalse();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
  }

  @Test
  void constructorWithDefaultClient() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThat(factory.isRunning()).isTrue();
  }

  @Test
  void constructorWithHttpClient() {
    HttpClient httpClient = HttpClient.create();
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(httpClient);
    assertThat(factory.isRunning()).isTrue();
  }

  @Test
  void constructorWithNullHttpClientThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ReactorClientHttpRequestFactory(null))
            .withMessage("HttpClient is required");
  }

  @Test
  void constructorWithResourceFactoryAndMapperWhenResourceFactoryNotRunning() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
    assertThat(factory.isRunning()).isFalse();
  }

  @Test
  void constructorWithResourceFactoryAndMapperWhenResourceFactoryRunning() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    resourceFactory.afterPropertiesSet();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
    assertThat(factory.isRunning()).isTrue();
  }

  @Test
  void setConnectTimeoutWithValidValue() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    // Should not throw exception
  }

  @Test
  void setConnectTimeoutWithZeroValue() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.setConnectTimeout(0);
    // Should not throw exception
  }

  @Test
  void setConnectTimeoutWithNegativeValueThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectTimeout(-1))
            .withMessage("Timeout must be a non-negative value");
  }

  @Test
  void setConnectTimeoutWithDuration() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    // Should not throw exception
  }

  @Test
  void setConnectTimeoutWithNullDurationThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setConnectTimeout((Duration) null))
            .withMessage("ConnectTimeout is required");
  }

  @Test
  void setReadTimeoutWithDuration() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(10));
    // Should not throw exception
  }

  @Test
  void setReadTimeoutWithZeroValueThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setReadTimeout(0L))
            .withMessage("Timeout must be a positive value");
  }

  @Test
  void setReadTimeoutWithNegativeValueThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setReadTimeout(-1L))
            .withMessage("Timeout must be a positive value");
  }

  @Test
  void setReadTimeoutWithNullDurationThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setReadTimeout((Duration) null))
            .withMessage("ReadTimeout is required");
  }

  @Test
  void setReadTimeoutWithLongValue() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.setReadTimeout(10000L);
    // Should not throw exception
  }

  @Test
  void setExchangeTimeoutWithValidValue() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.setExchangeTimeout(10000L);
    // Should not throw exception
  }

  @Test
  void setExchangeTimeoutWithZeroValueThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setExchangeTimeout(0L))
            .withMessage("Timeout must be a positive value");
  }

  @Test
  void setExchangeTimeoutWithNegativeValueThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setExchangeTimeout(-1L))
            .withMessage("Timeout must be a positive value");
  }

  @Test
  void setExchangeTimeoutWithDuration() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.setExchangeTimeout(Duration.ofSeconds(10));
    // Should not throw exception
  }

  @Test
  void setExchangeTimeoutWithNullDurationThrowsException() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setExchangeTimeout(null))
            .withMessage("ExchangeTimeout is required");
  }

  @Test
  void createRequestWithValidUriAndMethod() throws Exception {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    ClientHttpRequest request = factory.createRequest(new URI("http://example.com"), HttpMethod.GET);
    assertThat(request).isNotNull();
    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(request.getURI().toString()).isEqualTo("http://example.com");
  }

  @Test
  void createRequestWithExternalResourceFactory() throws Exception {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    resourceFactory.afterPropertiesSet();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
    ClientHttpRequest request = factory.createRequest(new URI("http://example.com"), HttpMethod.POST);
    assertThat(request).isNotNull();
  }

  @Test
  void createRequestWithStoppedExternalResourceFactory() throws Exception {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    resourceFactory.afterPropertiesSet();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
    factory.stop();
    ClientHttpRequest request = factory.createRequest(new URI("http://example.com"), HttpMethod.PUT);
    assertThat(request).isNotNull();
  }

  @Test
  void getPhaseReturnsCorrectValue() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThat(factory.getPhase()).isEqualTo(1);
  }

  @Test
  void isRunningReturnsFalseWhenHttpClientIsNull() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);
    assertThat(factory.isRunning()).isFalse();
  }

  @Test
  void isRunningReturnsTrueWhenHttpClientIsNotNull() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    assertThat(factory.isRunning()).isTrue();
  }

  @Test
  void startWhenResourceFactoryAndMapperAreNull() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.start();
    assertThat(factory.isRunning()).isTrue();
  }

  @Test
  void stopWhenResourceFactoryAndMapperAreNull() {
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory();
    factory.stop();
    assertThat(factory.isRunning()).isTrue();
  }

  @Test
  void stopAndStartWithExternalResourceFactory() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    resourceFactory.afterPropertiesSet();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(resourceFactory, mapper);

    factory.stop();
    assertThat(factory.isRunning()).isFalse();

    factory.start();
    assertThat(factory.isRunning()).isTrue();
  }

}
