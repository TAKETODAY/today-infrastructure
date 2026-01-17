/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.http.reactive.client;

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