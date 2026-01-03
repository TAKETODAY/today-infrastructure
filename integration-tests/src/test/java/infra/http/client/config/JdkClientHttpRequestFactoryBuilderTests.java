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

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

import infra.core.task.SimpleAsyncTaskExecutor;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdkClientHttpRequestFactoryBuilder} and {@link JdkHttpClientBuilder}.
 *
 * @author Phillip Webb
 */
class JdkClientHttpRequestFactoryBuilderTests
        extends AbstractClientHttpRequestFactoryBuilderTests<JdkClientHttpRequestFactory> {

  JdkClientHttpRequestFactoryBuilderTests() {
    super(JdkClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.jdk());
  }

  @Test
  void withCustomizers() {
    TestCustomizer<HttpClient.Builder> httpClientCustomizer1 = new TestCustomizer<>();
    TestCustomizer<HttpClient.Builder> httpClientCustomizer2 = new TestCustomizer<>();
    ClientHttpRequestFactoryBuilder.jdk()
            .withHttpClientCustomizer(httpClientCustomizer1)
            .withHttpClientCustomizer(httpClientCustomizer2)
            .build();
    httpClientCustomizer1.assertCalled();
    httpClientCustomizer2.assertCalled();
  }

  @Test
  void withExecutor() {
    Executor executor = new SimpleAsyncTaskExecutor();
    JdkClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.jdk().withExecutor(executor).build();
    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(factory, "httpClient");
    assertThat(httpClient).isNotNull();
    assertThat(httpClient.executor()).containsSame(executor);
  }

  @Test
  void with() {
    TestCustomizer<HttpClient.Builder> customizer = new TestCustomizer<>();
    ClientHttpRequestFactoryBuilder.jdk().with((builder) -> builder.withHttpClientCustomizer(customizer)).build();
    customizer.assertCalled();
  }

  @Override
  protected long connectTimeout(JdkClientHttpRequestFactory requestFactory) {
    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
    assertThat(httpClient).isNotNull();
    return httpClient.connectTimeout().get().toMillis();
  }

  @Override
  protected long readTimeout(JdkClientHttpRequestFactory requestFactory) {
    Duration readTimeout = (Duration) ReflectionTestUtils.getField(requestFactory, "readTimeout");
    assertThat(readTimeout).isNotNull();
    return readTimeout.toMillis();
  }

}
