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

package infra.http.client.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Duration;

import infra.http.HttpMethod;
import infra.http.client.BufferingClientHttpRequestFactory;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ReflectiveComponentsClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 */
class ReflectiveComponentsClientHttpRequestFactoryBuilderTests extends AbstractClientHttpRequestFactoryBuilderTests<ClientHttpRequestFactory> {

  ReflectiveComponentsClientHttpRequestFactoryBuilderTests() {
    super(ClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.of(ReflectiveClientHttpRequestFactory::new));
  }

  @Override
  void connectWithSslBundle(String httpMethod) throws Exception {
    HttpClientSettings settings = HttpClientSettings.ofSslBundle(sslBundle());
    assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
            .withMessage("Unable to set SSL bundler using reflection");
  }

  @Override
  void redirectFollow(String httpMethod) throws Exception {
    HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.FOLLOW);
    assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
            .withMessage("Unable to set redirect follow using reflection");
  }

  @Override
  void redirectDontFollow(String httpMethod) throws Exception {
    HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
    assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
            .withMessage("Unable to set redirect follow using reflection");
  }

  @Override
  @ParameterizedTest
  @ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
  void redirectDefault(String httpMethod) throws Exception {
    testRedirect(null, HttpMethod.valueOf(httpMethod), ALWAYS_FOUND);
  }

  @Override
  void connectWithSslBundleAndOptionsMismatch(String httpMethod) throws Exception {
    assertThatIllegalStateException().isThrownBy(() -> super.connectWithSslBundleAndOptionsMismatch(httpMethod))
            .withMessage("Unable to set SSL bundler using reflection");
  }

  @Test
  void buildWithClassCreatesFactory() {
    assertThat(ofTestRequestFactory().build()).isInstanceOf(TestClientHttpRequestFactory.class);
  }

  @Test
  void buildWithClassWhenHasConnectTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
    TestClientHttpRequestFactory requestFactory = ofTestRequestFactory().build(settings);
    assertThat(requestFactory.connectTimeout).isEqualTo(Duration.ofSeconds(60).toMillis());
  }

  @Test
  void buildWithClassWhenHasReadTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(90));
    TestClientHttpRequestFactory requestFactory = ofTestRequestFactory().build(settings);
    assertThat(requestFactory.readTimeout).isEqualTo(Duration.ofSeconds(90).toMillis());
  }

  @Test
  void buildWithClassWhenUnconfigurableTypeWithConnectTimeoutThrowsException() {
    HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
    assertThatIllegalStateException().isThrownBy(() -> ofUnconfigurableRequestFactory().build(settings))
            .withMessageContaining("suitable setConnectTimeout method");
  }

  @Test
  void buildWithClassWhenUnconfigurableTypeWithReadTimeoutThrowsException() {
    HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(60));
    assertThatIllegalStateException().isThrownBy(() -> ofUnconfigurableRequestFactory().build(settings))
            .withMessageContaining("suitable setReadTimeout method");
  }

  @Test
  void buildWithClassWhenDeprecatedMethodsTypeWithConnectTimeoutThrowsException() {
    HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
    assertThatIllegalStateException().isThrownBy(() -> ofDeprecatedMethodsRequestFactory().build(settings))
            .withMessageContaining("setConnectTimeout method marked as deprecated");
  }

  @Test
  void buildWithClassWhenDeprecatedMethodsTypeWithReadTimeoutThrowsException() {
    HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(60));
    assertThatIllegalStateException().isThrownBy(() -> ofDeprecatedMethodsRequestFactory().build(settings))
            .withMessageContaining("setReadTimeout method marked as deprecated");
  }

  @Test
  void buildWithSupplierWhenWrappedRequestFactoryTypeWithConnectTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofMillis(1234));
    JdkClientHttpRequestFactory wrappedRequestFactory = new ReflectiveClientHttpRequestFactory();
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
            .of(() -> new BufferingClientHttpRequestFactory(wrappedRequestFactory))
            .build(settings);
    assertThat(requestFactory).extracting("requestFactory").isSameAs(wrappedRequestFactory);
    assertThat(wrappedRequestFactory).hasFieldOrPropertyWithValue("connectTimeoutDuration", Duration.ofMillis(1234));
  }

  @Test
  void buildWithSupplierWhenWrappedRequestFactoryTypeWithReadTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofMillis(1234));
    JdkClientHttpRequestFactory wrappedRequestFactory = new ReflectiveClientHttpRequestFactory();
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
            .of(() -> new BufferingClientHttpRequestFactory(wrappedRequestFactory))
            .build(settings);
    assertThat(requestFactory).extracting("requestFactory").isSameAs(wrappedRequestFactory);
    assertThat(wrappedRequestFactory).hasFieldOrPropertyWithValue("readTimeout", Duration.ofMillis(1234));
  }

  @Test
  void buildWithClassWhenHasMultipleTimeoutSettersFavorsDurationMethods() {
    HttpClientSettings settings = HttpClientSettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(1))
            .withReadTimeout(Duration.ofSeconds(2));
    IntAndDurationTimeoutsClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
            .of(IntAndDurationTimeoutsClientHttpRequestFactory.class)
            .build(settings);
    assertThat((requestFactory).connectTimeout).isZero();
    assertThat((requestFactory).readTimeout).isZero();
    assertThat((requestFactory).connectTimeoutDuration).isEqualTo(Duration.ofSeconds(1));
    assertThat((requestFactory).readTimeoutDuration).isEqualTo(Duration.ofSeconds(2));
  }

  private ClientHttpRequestFactoryBuilder<TestClientHttpRequestFactory> ofTestRequestFactory() {
    return ClientHttpRequestFactoryBuilder.of(TestClientHttpRequestFactory.class);
  }

  private ClientHttpRequestFactoryBuilder<UnconfigurableClientHttpRequestFactory> ofUnconfigurableRequestFactory() {
    return ClientHttpRequestFactoryBuilder.of(UnconfigurableClientHttpRequestFactory.class);
  }

  private ClientHttpRequestFactoryBuilder<DeprecatedMethodsClientHttpRequestFactory> ofDeprecatedMethodsRequestFactory() {
    return ClientHttpRequestFactoryBuilder.of(DeprecatedMethodsClientHttpRequestFactory.class);
  }

  @Override
  protected long connectTimeout(ClientHttpRequestFactory requestFactory) {
    ReflectiveClientHttpRequestFactory factory = (ReflectiveClientHttpRequestFactory) requestFactory;
    return factory.connectTimeoutDuration.toMillis();
  }

  @Override
  protected long readTimeout(ClientHttpRequestFactory requestFactory) {
    Object field = ReflectionTestUtils.getField(requestFactory, "readTimeout");
    assertThat(field).isNotNull();
    if (field instanceof Duration) {
      return ((Duration) field).toMillis();
    }
    return (long) field;
  }

  public static class TestClientHttpRequestFactory implements ClientHttpRequestFactory {

    private int connectTimeout;

    private int readTimeout;

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
      throw new UnsupportedOperationException();
    }

    public void setConnectTimeout(int timeout) {
      this.connectTimeout = timeout;
    }

    public void setReadTimeout(int timeout) {
      this.readTimeout = timeout;
    }

  }

  public static class UnconfigurableClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
      throw new UnsupportedOperationException();
    }

  }

  public static class DeprecatedMethodsClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
      throw new UnsupportedOperationException();
    }

    @Deprecated(since = "3.0.0", forRemoval = false)
    public void setConnectTimeout(int timeout) {
    }

    @Deprecated(since = "3.0.0", forRemoval = false)
    public void setReadTimeout(int timeout) {
    }

    @Deprecated(since = "3.0.0", forRemoval = false)
    public void setBufferRequestBody(boolean bufferRequestBody) {
    }

  }

  public static class IntAndDurationTimeoutsClientHttpRequestFactory implements ClientHttpRequestFactory {

    private int readTimeout;

    private int connectTimeout;

    private @Nullable Duration readTimeoutDuration;

    private @Nullable Duration connectTimeoutDuration;

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
      throw new UnsupportedOperationException();
    }

    public void setConnectTimeout(int timeout) {
      this.connectTimeout = timeout;
    }

    public void setReadTimeout(int timeout) {
      this.readTimeout = timeout;
    }

    public void setConnectTimeout(@Nullable Duration timeout) {
      this.connectTimeoutDuration = timeout;
    }

    public void setReadTimeout(@Nullable Duration timeout) {
      this.readTimeoutDuration = timeout;
    }

  }

  public static class ReflectiveClientHttpRequestFactory extends JdkClientHttpRequestFactory {

    private int connectTimeout;

    private @Nullable Duration connectTimeoutDuration;

    public void setConnectTimeout(int timeout) {
      this.connectTimeout = timeout;
    }

    public void setConnectTimeout(@Nullable Duration timeout) {
      this.connectTimeoutDuration = timeout;
    }

  }

}
