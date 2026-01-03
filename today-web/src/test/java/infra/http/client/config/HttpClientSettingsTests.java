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

  private static final Duration TWO_SECONDS = Duration.ofSeconds(2);

  @Test
  void defaults() {
    HttpClientSettings settings = HttpClientSettings.defaults();
    assertThat(settings.redirects()).isNull();
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void createWithNulls() {
    HttpClientSettings settings = new HttpClientSettings(null, null, null, null);
    assertThat(settings.redirects()).isNull();
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withConnectTimeoutReturnsInstanceWithUpdatedConnectionTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(ONE_SECOND);
    assertThat(settings.redirects()).isNull();
    assertThat(settings.connectTimeout()).isEqualTo(ONE_SECOND);
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withReadTimeoutReturnsInstanceWithUpdatedReadTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(ONE_SECOND);
    assertThat(settings.redirects()).isNull();
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isEqualTo(ONE_SECOND);
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withSslBundleReturnsInstanceWithUpdatedSslBundle() {
    SslBundle sslBundle = mock(SslBundle.class);
    HttpClientSettings settings = HttpClientSettings.defaults().withSslBundle(sslBundle);
    assertThat(settings.redirects()).isNull();
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.sslBundle()).isSameAs(sslBundle);
  }

  @Test
  void withRedirectsReturnsInstanceWithUpdatedRedirect() {
    HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
    assertThat(settings.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void orElseReturnsNewInstanceWithUpdatedValues() {
    SslBundle sslBundle = mock(SslBundle.class);
    HttpClientSettings settings = new HttpClientSettings(null, ONE_SECOND, null, null)
            .orElse(new HttpClientSettings(HttpRedirects.FOLLOW_WHEN_POSSIBLE, TWO_SECONDS, TWO_SECONDS, sslBundle));
    assertThat(settings.redirects()).isEqualTo(HttpRedirects.FOLLOW_WHEN_POSSIBLE);
    assertThat(settings.connectTimeout()).isEqualTo(ONE_SECOND);
    assertThat(settings.readTimeout()).isEqualTo(TWO_SECONDS);
    assertThat(settings.sslBundle()).isEqualTo(sslBundle);
  }

  @Test
  void ofSslBundleCreatesNewSettings() {
    SslBundle sslBundle = mock(SslBundle.class);
    HttpClientSettings settings = HttpClientSettings.ofSslBundle(sslBundle);
    assertThat(settings.redirects()).isNull();
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.sslBundle()).isSameAs(sslBundle);
  }

}