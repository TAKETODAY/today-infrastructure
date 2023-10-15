/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.function.Function;

import cn.taketoday.http.HttpMethod;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/14 16:33
 */
class ReactorNettyClientRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new ReactorNettyClientRequestFactory();
  }

  @Override
  @Test
  public void httpMethods() throws Exception {
    super.httpMethods();
    assertHttpMethod("patch", HttpMethod.PATCH);
  }

  @Test
  void restartWithDefaultConstructor() {
    ReactorNettyClientRequestFactory requestFactory = new ReactorNettyClientRequestFactory();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.stop();
    assertThat(requestFactory.isRunning()).isFalse();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
  }

  @Test
  void restartWithExternalResourceFactory() {
    ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
    resourceFactory.afterPropertiesSet();
    Function<HttpClient, HttpClient> mapper = Function.identity();
    ReactorNettyClientRequestFactory requestFactory = new ReactorNettyClientRequestFactory(resourceFactory, mapper);
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.stop();
    assertThat(requestFactory.isRunning()).isFalse();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
  }

  @Test
  void restartWithHttpClient() {
    HttpClient httpClient = HttpClient.create();
    ReactorNettyClientRequestFactory requestFactory = new ReactorNettyClientRequestFactory(httpClient);
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
    requestFactory.stop();
    assertThat(requestFactory.isRunning()).isFalse();
    requestFactory.start();
    assertThat(requestFactory.isRunning()).isTrue();
  }

}
