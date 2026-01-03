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

package infra.http.client.config.reactive;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import infra.http.client.ReactorResourceFactory;
import infra.http.client.config.ReactorHttpClientBuilder;
import infra.http.client.reactive.ReactorClientHttpConnector;
import infra.test.util.ReflectionTestUtils;
import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ReactorClientHttpConnectorBuilder} and
 * {@link ReactorHttpClientBuilder}.
 *
 * @author Phillip Webb
 */
class ReactorClientHttpConnectorBuilderTests
        extends AbstractClientHttpConnectorBuilderTests<ReactorClientHttpConnector> {

  ReactorClientHttpConnectorBuilderTests() {
    super(ReactorClientHttpConnector.class, ClientHttpConnectorBuilder.reactor());
  }

  @Test
  void withHttpClientFactory() {
    boolean[] called = new boolean[1];
    Supplier<HttpClient> httpClientFactory = () -> {
      called[0] = true;
      return HttpClient.create();
    };
    ClientHttpConnectorBuilder.reactor().withHttpClientFactory(httpClientFactory).build();
    assertThat(called).containsExactly(true);
  }

  @Test
  void withReactorResourceFactory() {
    ReactorResourceFactory resourceFactory = spy(new ReactorResourceFactory());
    ClientHttpConnectorBuilder.reactor().withReactorResourceFactory(resourceFactory).build();
    then(resourceFactory).should().getConnectionProvider();
    then(resourceFactory).should().getLoopResources();
  }

  @Test
  void withCustomizers() {
    List<HttpClient> httpClients = new ArrayList<>();
    UnaryOperator<HttpClient> httpClientCustomizer1 = (httpClient) -> {
      httpClients.add(httpClient);
      return httpClient;
    };
    UnaryOperator<HttpClient> httpClientCustomizer2 = (httpClient) -> {
      httpClients.add(httpClient);
      return httpClient;
    };
    ClientHttpConnectorBuilder.reactor()
            .withHttpClientCustomizer(httpClientCustomizer1)
            .withHttpClientCustomizer(httpClientCustomizer2)
            .build();
    assertThat(httpClients).hasSize(2);
  }

  @Test
  void with() {
    boolean[] called = new boolean[1];
    Supplier<HttpClient> httpClientFactory = () -> {
      called[0] = true;
      return HttpClient.create();
    };
    ClientHttpConnectorBuilder.reactor()
            .with((builder) -> builder.withHttpClientFactory(httpClientFactory))
            .build();
    assertThat(called).containsExactly(true);
  }

  @Override
  protected long connectTimeout(ReactorClientHttpConnector connector) {
    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
    assertThat(httpClient).isNotNull();
    Object connectTimeout = httpClient.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS);
    assertThat(connectTimeout).isNotNull();
    return (int) connectTimeout;
  }

  @Override
  protected long readTimeout(ReactorClientHttpConnector connector) {
    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
    assertThat(httpClient).isNotNull();
    Duration responseTimeout = httpClient.configuration().responseTimeout();
    assertThat(responseTimeout).isNotNull();
    return (int) responseTimeout.toMillis();
  }

}
