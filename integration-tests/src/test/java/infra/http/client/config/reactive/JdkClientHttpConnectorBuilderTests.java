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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

import infra.core.task.SimpleAsyncTaskExecutor;
import infra.http.client.config.JdkHttpClientBuilder;
import infra.http.client.reactive.JdkClientHttpConnector;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdkClientHttpConnectorBuilder} and {@link JdkHttpClientBuilder}.
 *
 * @author Phillip Webb
 */
class JdkClientHttpConnectorBuilderTests extends AbstractClientHttpConnectorBuilderTests<JdkClientHttpConnector> {

  JdkClientHttpConnectorBuilderTests() {
    super(JdkClientHttpConnector.class, ClientHttpConnectorBuilder.jdk());
  }

  @Test
  void withCustomizers() {
    TestCustomizer<HttpClient.Builder> httpClientCustomizer1 = new TestCustomizer<>();
    TestCustomizer<HttpClient.Builder> httpClientCustomizer2 = new TestCustomizer<>();
    ClientHttpConnectorBuilder.jdk()
            .withHttpClientCustomizer(httpClientCustomizer1)
            .withHttpClientCustomizer(httpClientCustomizer2)
            .build();
    httpClientCustomizer1.assertCalled();
    httpClientCustomizer2.assertCalled();
  }

  @Test
  void withExecutor() {
    Executor executor = new SimpleAsyncTaskExecutor();
    JdkClientHttpConnector connector = ClientHttpConnectorBuilder.jdk().withExecutor(executor).build();
    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
    assertThat(httpClient).isNotNull();
    assertThat(httpClient.executor()).containsSame(executor);
  }

  @Test
  void with() {
    TestCustomizer<HttpClient.Builder> customizer = new TestCustomizer<>();
    ClientHttpConnectorBuilder.jdk().with((builder) -> builder.withHttpClientCustomizer(customizer)).build();
    customizer.assertCalled();
  }

  @Override
  protected long connectTimeout(JdkClientHttpConnector connector) {
    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
    assertThat(httpClient).isNotNull();
    return httpClient.connectTimeout().get().toMillis();
  }

  @Override
  protected long readTimeout(JdkClientHttpConnector connector) {
    Duration readTimeout = (Duration) ReflectionTestUtils.getField(connector, "readTimeout");
    assertThat(readTimeout).isNotNull();
    return readTimeout.toMillis();
  }

}
