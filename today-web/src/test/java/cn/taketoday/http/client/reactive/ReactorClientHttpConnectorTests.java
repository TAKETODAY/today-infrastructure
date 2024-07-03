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

package cn.taketoday.http.client.reactive;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.function.Function;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ReactorResourceFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/11 22:18
 */
class ReactorClientHttpConnectorTests {

  @Test
  void restartWithDefaultConstructor() {
    ReactorClientHttpConnector connector = new ReactorClientHttpConnector();
    assertThat(connector.isRunning()).isTrue();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isTrue();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isTrue();
  }

  @Test
  void restartWithHttpClient() {
    HttpClient httpClient = HttpClient.create();
    ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
    assertThat(connector.isRunning()).isTrue();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isTrue();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isTrue();
  }

  @Test
  void restartWithExternalResourceFactory() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    resourceFactory.afterPropertiesSet();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory, mapper);
    assertThat(connector.isRunning()).isTrue();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isFalse();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isFalse();
  }

  @Test
  void lateStartWithExternalResourceFactory() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory, mapper);
    assertThat(connector.isRunning()).isFalse();
    resourceFactory.start();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isFalse();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isFalse();
  }

  @Test
  void lazyStartWithExternalResourceFactory() throws Exception {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory, mapper);
    assertThat(connector.isRunning()).isFalse();
    resourceFactory.start();
    connector.connect(HttpMethod.GET, new URI(""), request -> Mono.empty());
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isFalse();
    connector.connect(HttpMethod.GET, new URI(""), request -> Mono.empty());
    assertThat(connector.isRunning()).isFalse();
    connector.start();
    assertThat(connector.isRunning()).isTrue();
    connector.stop();
    assertThat(connector.isRunning()).isFalse();
  }

}