/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.client.reactive;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Function;

import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpMethod;
import infra.http.ResponseCookie;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 13:40
 */
class JdkClientHttpConnectorTests {

  @Test
  void constructorWithDefaultHttpClient() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    assertThat(connector).isNotNull();
  }

  @Test
  void constructorWithCustomHttpClient() {
    HttpClient httpClient = HttpClient.newHttpClient();
    JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
    assertThat(connector).isNotNull();
  }

  @Test
  void constructorWithBuilderAndNullResourceFactory() {
    HttpClient.Builder builder = HttpClient.newBuilder();
    JdkClientHttpConnector connector = new JdkClientHttpConnector(builder, null);
    assertThat(connector).isNotNull();
  }

  @Test
  void setBufferFactoryWithValidFactory() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    DataBufferFactory factory = mock(DataBufferFactory.class);
    connector.setBufferFactory(factory);
    // Should not throw exception
  }

  @Test
  void setBufferFactoryWithNullThrowsException() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> connector.setBufferFactory(null))
            .withMessage("DataBufferFactory is required");
  }

  @Test
  void setReadTimeoutWithValidDuration() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    Duration timeout = Duration.ofSeconds(10);
    connector.setReadTimeout(timeout);
    // Should not throw exception
  }

  @Test
  void setReadTimeoutWithNullThrowsException() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> connector.setReadTimeout(null))
            .withMessage("readTimeout is required");
  }

  @Test
  void setCookieParserWithValidParser() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    ResponseCookie.Parser parser = mock(ResponseCookie.Parser.class);
    connector.setCookieParser(parser);
    // Should not throw exception
  }

  @Test
  void setCookieParserWithNullThrowsException() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> connector.setCookieParser(null))
            .withMessage("ResponseCookie parser is required");
  }

  @Test
  void connectReturnsMonoWithHttpResponse() {
    JdkClientHttpConnector connector = new JdkClientHttpConnector();
    HttpMethod method = HttpMethod.GET;
    URI uri = URI.create("http://example.com");
    Function<ClientHttpRequest, Mono<Void>> requestCallback = request -> Mono.empty();

    Mono<ClientHttpResponse> responseMono = connector.connect(method, uri, requestCallback);
    assertThat(responseMono).isNotNull();
  }

}