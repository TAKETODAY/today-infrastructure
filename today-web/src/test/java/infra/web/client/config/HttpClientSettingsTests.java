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

package infra.web.client.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.core.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/1 23:10
 */
class HttpClientSettingsTests {

  private static final Duration ONE_SECOND = Duration.ofSeconds(1);

  @Test
  void defaultsHasNullValues() {
    HttpClientSettings settings = HttpClientSettings.DEFAULTS;
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withConnectTimeoutReturnsInstanceWithUpdatedConnectionTimeout() {
    HttpClientSettings settings = HttpClientSettings.DEFAULTS
            .withConnectTimeout(ONE_SECOND);
    assertThat(settings.connectTimeout()).isEqualTo(ONE_SECOND);
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withReadTimeoutReturnsInstanceWithUpdatedReadTimeout() {
    HttpClientSettings settings = HttpClientSettings.DEFAULTS
            .withReadTimeout(ONE_SECOND);
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isEqualTo(ONE_SECOND);
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withBufferRequestBodyReturnsInstanceWithUpdatedBufferRequestBody() {
    HttpClientSettings settings = HttpClientSettings.DEFAULTS
            .withBufferRequestBody(true);
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isTrue();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withSslBundleReturnsInstanceWithUpdatedSslBundle() {
    SslBundle sslBundle = mock(SslBundle.class);
    HttpClientSettings settings = HttpClientSettings.DEFAULTS.withSslBundle(sslBundle);
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isSameAs(sslBundle);
  }

}