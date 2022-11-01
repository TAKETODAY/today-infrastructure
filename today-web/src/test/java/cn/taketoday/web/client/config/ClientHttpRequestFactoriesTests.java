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

package cn.taketoday.web.client.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.BufferingClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/1 22:59
 */
class ClientHttpRequestFactoriesTests {

  @Test
  void getReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }

  @Test
  void getOfGeneralTypeReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }

  @Test
  void getOfSimpleFactoryReturnsSimpleFactory() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(SimpleClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
  }

  @Test
  void getOfHttpComponentsFactoryReturnsHttpComponentsFactory() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(HttpComponentsClientHttpRequestFactory.class, ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }

  @Test
  void getOfOkHttpFactoryReturnsOkHttpFactory() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(OkHttp3ClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(OkHttp3ClientHttpRequestFactory.class);
  }

  @Test
  void getOfUnknownTypeCreatesFactory() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(TestClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(TestClientHttpRequestFactory.class);
  }

  @Test
  void getOfUnknownTypeWithConnectTimeoutCreatesFactoryAndConfiguresConnectTimeout() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(TestClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(60)));
    assertThat(requestFactory).isInstanceOf(TestClientHttpRequestFactory.class);
    assertThat(((TestClientHttpRequestFactory) requestFactory).connectTimeout)
            .isEqualTo(Duration.ofSeconds(60).toMillis());
  }

  @Test
  void getOfUnknownTypeWithReadTimeoutCreatesFactoryAndConfiguresReadTimeout() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(TestClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(90)));
    assertThat(requestFactory).isInstanceOf(TestClientHttpRequestFactory.class);
    assertThat(((TestClientHttpRequestFactory) requestFactory).readTimeout)
            .isEqualTo(Duration.ofSeconds(90).toMillis());
  }

  @Test
  void getOfUnknownTypeWithBodyBufferingCreatesFactoryAndConfiguresBodyBuffering() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(TestClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS.withBufferRequestBody(true));
    assertThat(requestFactory).isInstanceOf(TestClientHttpRequestFactory.class);
    assertThat(((TestClientHttpRequestFactory) requestFactory).bufferRequestBody).isTrue();
  }

  @Test
  void getOfUnconfigurableTypeWithConnectTimeoutThrows() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ClientHttpRequestFactories.get(UnconfigurableClientHttpRequestFactory.class,
                    ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(60))))
            .withMessageContaining("suitable setConnectTimeout method");
  }

  @Test
  void getOfUnconfigurableTypeWithReadTimeoutThrows() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ClientHttpRequestFactories.get(UnconfigurableClientHttpRequestFactory.class,
                    ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(60))))
            .withMessageContaining("suitable setReadTimeout method");
  }

  @Test
  void getOfUnconfigurableTypeWithBodyBufferingThrows() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ClientHttpRequestFactories.get(UnconfigurableClientHttpRequestFactory.class,
                    ClientHttpRequestFactorySettings.DEFAULTS.withBufferRequestBody(true)))
            .withMessageContaining("suitable setBufferRequestBody method");
  }

  @Test
  void getOfTypeWithDeprecatedConnectTimeoutThrowsWithConnectTimeout() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ClientHttpRequestFactories.get(DeprecatedMethodsClientHttpRequestFactory.class,
                    ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(60))))
            .withMessageContaining("setConnectTimeout method marked as deprecated");
  }

  @Test
  void getOfTypeWithDeprecatedReadTimeoutThrowsWithReadTimeout() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ClientHttpRequestFactories.get(DeprecatedMethodsClientHttpRequestFactory.class,
                    ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(60))))
            .withMessageContaining("setReadTimeout method marked as deprecated");
  }

  @Test
  void getOfTypeWithDeprecatedBufferRequestBodyThrowsWithBufferRequestBody() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ClientHttpRequestFactories.get(DeprecatedMethodsClientHttpRequestFactory.class,
                    ClientHttpRequestFactorySettings.DEFAULTS.withBufferRequestBody(false)))
            .withMessageContaining("setBufferRequestBody method marked as deprecated");
  }

  @Test
  void connectTimeoutCanBeConfiguredOnAWrappedRequestFactory() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    BufferingClientHttpRequestFactory result = ClientHttpRequestFactories.get(
            () -> new BufferingClientHttpRequestFactory(requestFactory),
            ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofMillis(1234)));
    assertThat(result).extracting("requestFactory").isSameAs(requestFactory);
    assertThat(requestFactory).hasFieldOrPropertyWithValue("connectTimeout", 1234);
  }

  @Test
  void readTimeoutCanBeConfiguredOnAWrappedRequestFactory() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    BufferingClientHttpRequestFactory result = ClientHttpRequestFactories.get(
            () -> new BufferingClientHttpRequestFactory(requestFactory),
            ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofMillis(1234)));
    assertThat(result).extracting("requestFactory").isSameAs(requestFactory);
    assertThat(requestFactory).hasFieldOrPropertyWithValue("readTimeout", 1234);
  }

  @Test
  void bufferRequestBodyCanBeConfiguredOnAWrappedRequestFactory() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    assertThat(requestFactory).hasFieldOrPropertyWithValue("bufferRequestBody", true);
    BufferingClientHttpRequestFactory result = ClientHttpRequestFactories.get(
            () -> new BufferingClientHttpRequestFactory(requestFactory),
            ClientHttpRequestFactorySettings.DEFAULTS.withBufferRequestBody(false));
    assertThat(result).extracting("requestFactory").isSameAs(requestFactory);
    assertThat(requestFactory).hasFieldOrPropertyWithValue("bufferRequestBody", false);
  }

  public static class TestClientHttpRequestFactory implements ClientHttpRequestFactory {

    private int connectTimeout;

    private int readTimeout;

    private boolean bufferRequestBody;

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
      throw new UnsupportedOperationException();
    }

    public void setConnectTimeout(int timeout) {
      this.connectTimeout = timeout;
    }

    public void setReadTimeout(int timeout) {
      this.readTimeout = timeout;
    }

    public void setBufferRequestBody(boolean bufferRequestBody) {
      this.bufferRequestBody = bufferRequestBody;
    }

  }

  public static class UnconfigurableClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
      throw new UnsupportedOperationException();
    }

  }

  public static class DeprecatedMethodsClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
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

}