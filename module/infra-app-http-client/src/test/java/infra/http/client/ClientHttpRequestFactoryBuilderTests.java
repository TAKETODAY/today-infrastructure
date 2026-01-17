/*
 * Copyright 2012-present the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import infra.http.HttpMethod;
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
