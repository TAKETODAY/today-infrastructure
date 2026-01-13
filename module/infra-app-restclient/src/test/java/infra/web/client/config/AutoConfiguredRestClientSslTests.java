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

package infra.web.client.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.function.Consumer;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.HttpRedirects;
import infra.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfiguredRestClientSsl}.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
class AutoConfiguredRestClientSslTests {

  private final HttpClientSettings settings = HttpClientSettings
          .ofSslBundle(mock(SslBundle.class, "Default SslBundle"))
          .withRedirects(HttpRedirects.DONT_FOLLOW)
          .withReadTimeout(Duration.ofSeconds(10))
          .withConnectTimeout(Duration.ofSeconds(30));

  @Mock
  @SuppressWarnings("NullAway.Init")
  private SslBundles sslBundles;

  @Mock
  @SuppressWarnings("NullAway.Init")
  private ClientHttpRequestFactoryBuilder<ClientHttpRequestFactory> factoryBuilder;

  @Mock
  @SuppressWarnings("NullAway.Init")
  private ClientHttpRequestFactory factory;

  private AutoConfiguredRestClientSsl restClientSsl;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    this.restClientSsl = new AutoConfiguredRestClientSsl(this.factoryBuilder, this.settings, this.sslBundles);
  }

  @Test
  void shouldConfigureRestClientUsingBundleName() {
    String bundleName = "test";
    SslBundle sslBundle = mock(SslBundle.class, "SslBundle named '%s'".formatted(bundleName));
    given(this.sslBundles.getBundle(bundleName)).willReturn(sslBundle);
    given(this.factoryBuilder.build(this.settings.withSslBundle(sslBundle))).willReturn(this.factory);
    RestClient restClient = build(this.restClientSsl.fromBundle(bundleName));
    assertThat(restClient).hasFieldOrPropertyWithValue("clientRequestFactory", this.factory);
  }

  @Test
  void shouldConfigureRestClientUsingBundle() {
    SslBundle sslBundle = mock(SslBundle.class, "Custom SslBundle");
    given(this.factoryBuilder.build(this.settings.withSslBundle(sslBundle))).willReturn(this.factory);
    RestClient restClient = build(this.restClientSsl.fromBundle(sslBundle));
    assertThat(restClient).hasFieldOrPropertyWithValue("clientRequestFactory", this.factory);
  }

  private RestClient build(Consumer<RestClient.Builder> customizer) {
    RestClient.Builder builder = RestClient.builder();
    customizer.accept(builder);
    return builder.build();
  }

}
