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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import infra.http.HttpMethod;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.ReactorClientHttpRequestFactory;
import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 */
class ClientHttpRequestFactoryBuilderTests {

  @Test
  void withCustomizerAppliesCustomizers() {
    ClientHttpRequestFactoryBuilder<JdkClientHttpRequestFactory> builder = (settings) -> new JdkClientHttpRequestFactory();
    builder = builder.withCustomizer(this::setReadTimeout);
    JdkClientHttpRequestFactory factory = builder.build(null);
    assertThat(factory).extracting("readTimeout").isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void withCustomizersAppliesCustomizers() {
    ClientHttpRequestFactoryBuilder<JdkClientHttpRequestFactory> builder = (settings) -> new JdkClientHttpRequestFactory();
    builder = builder.withCustomizers(List.of(this::setReadTimeout));
    JdkClientHttpRequestFactory factory = builder.build(null);
    assertThat(factory).extracting("readTimeout").isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void httpComponentsReturnsHttpComponentsFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.httpComponents())
            .isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void reactorReturnsReactorFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.reactor())
            .isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void jdkReturnsJdkFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.jdk()).isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void ofWhenExactlyClientHttpRequestFactoryTypeThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ClientHttpRequestFactoryBuilder.of(ClientHttpRequestFactory.class))
            .withMessage("'requestFactoryType' must be an implementation of ClientHttpRequestFactory");
  }

  @Test
  void ofWhenSimpleFactoryReturnsSimpleFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.of(JdkClientHttpRequestFactory.class))
            .isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void ofWhenHttpComponentsFactoryReturnsHttpComponentsFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.of(HttpComponentsClientHttpRequestFactory.class))
            .isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void ofWhenReactorFactoryReturnsReactorFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.of(ReactorClientHttpRequestFactory.class))
            .isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void ofWhenJdkFactoryReturnsJdkFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.of(JdkClientHttpRequestFactory.class))
            .isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void ofWhenUnknownTypeReturnsReflectiveFactoryBuilder() {
    ClientHttpRequestFactoryBuilder<TestClientHttpRequestFactory> builder = ClientHttpRequestFactoryBuilder
            .of(TestClientHttpRequestFactory.class);
    assertThat(builder).isInstanceOf(ReflectiveComponentsClientHttpRequestFactoryBuilder.class);
    assertThat(builder.build(null)).isInstanceOf(TestClientHttpRequestFactory.class);
  }

  @Test
  @SuppressWarnings("NullAway")
    // Test null check
  void ofWithSupplierWhenSupplierIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ClientHttpRequestFactoryBuilder.of((Supplier<ClientHttpRequestFactory>) null))
            .withMessage("'requestFactorySupplier' is required");
  }

  @Test
  void ofWithSupplierReturnsReflectiveFactoryBuilder() {
    assertThat(ClientHttpRequestFactoryBuilder.of(JdkClientHttpRequestFactory::new))
            .isInstanceOf(ReflectiveComponentsClientHttpRequestFactoryBuilder.class);
  }

  @Test
  void detectWhenHttpComponents() {
    assertThat(ClientHttpRequestFactoryBuilder.detect())
            .isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
  }

  @Test
  @ClassPathExclusions("httpclient5-*.jar")
  void detectWhen() {
    assertThat(ClientHttpRequestFactoryBuilder.detect()).isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
  }

  @Test
  @ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar" })
  void detectWhenReactor() {
    assertThat(ClientHttpRequestFactoryBuilder.detect()).isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
  }

  @Test
  @ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar", "reactor-netty-http-*.jar" })
  void detectWhenJdk() {
    assertThat(ClientHttpRequestFactoryBuilder.detect()).isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
  }

  private void setReadTimeout(JdkClientHttpRequestFactory factory) {
    factory.setReadTimeout(Duration.ofSeconds(5));
  }

  public static class TestClientHttpRequestFactory implements ClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
      throw new UnsupportedOperationException();
    }

  }

}
