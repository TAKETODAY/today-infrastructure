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

import org.jspecify.annotations.Nullable;

import java.time.Duration;

import infra.core.ssl.SslBundle;

/**
 * Settings that can be applied when creating an imperative or reactive HTTP client.
 *
 * @param redirects the follow redirect strategy to use or null to redirect whenever the
 * underlying library allows it
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param sslBundle the SSL bundle providing SSL configuration
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public record HttpClientSettings(@Nullable HttpRedirects redirects,
        @Nullable Duration connectTimeout, @Nullable Duration readTimeout, @Nullable SslBundle sslBundle) {

  private static final HttpClientSettings defaults = new HttpClientSettings(
          null, null, null, null);

  /**
   * Return a new {@link HttpClientSettings} instance with an updated connect timeout
   * setting.
   *
   * @param connectTimeout the new connect timeout setting
   * @return a new {@link HttpClientSettings} instance
   * @since 5.0
   */
  public HttpClientSettings withConnectTimeout(@Nullable Duration connectTimeout) {
    return new HttpClientSettings(this.redirects, connectTimeout, this.readTimeout, this.sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated read timeout
   * setting.
   *
   * @param readTimeout the new read timeout setting
   * @return a new {@link HttpClientSettings} instance
   * @since 5.0
   */
  public HttpClientSettings withReadTimeout(@Nullable Duration readTimeout) {
    return new HttpClientSettings(this.redirects, this.connectTimeout, readTimeout, this.sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated connect and read
   * timeout setting.
   *
   * @param connectTimeout the new connect timeout setting
   * @param readTimeout the new read timeout setting
   * @return a new {@link HttpClientSettings} instance
   * @since 5.0
   */
  public HttpClientSettings withTimeouts(@Nullable Duration connectTimeout, @Nullable Duration readTimeout) {
    return new HttpClientSettings(this.redirects, connectTimeout, readTimeout, this.sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated SSL bundle
   * setting.
   *
   * @param sslBundle the new SSL bundle setting
   * @return a new {@link HttpClientSettings} instance
   * @since 5.0
   */
  public HttpClientSettings withSslBundle(@Nullable SslBundle sslBundle) {
    return new HttpClientSettings(this.redirects, this.connectTimeout, this.readTimeout, sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated redirect setting.
   *
   * @param redirects the new redirects setting
   * @return a new {@link HttpClientSettings} instance
   * @since 5.0
   */
  public HttpClientSettings withRedirects(@Nullable HttpRedirects redirects) {
    return new HttpClientSettings(redirects, this.connectTimeout, this.readTimeout, this.sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance using values from this instance
   * when they are present, or otherwise using values from {@code other}.
   *
   * @param other the settings to be used to obtain values not present in this instance
   * @return a new {@link HttpClientSettings} instance
   * @since 5.0
   */
  public HttpClientSettings orElse(@Nullable HttpClientSettings other) {
    if (other == null) {
      return this;
    }
    HttpRedirects redirects = redirects() != null ? redirects() : other.redirects();
    Duration connectTimeout = connectTimeout() != null ? connectTimeout() : other.connectTimeout();
    Duration readTimeout = readTimeout() != null ? readTimeout() : other.readTimeout();
    SslBundle sslBundle = sslBundle() != null ? sslBundle() : other.sslBundle();
    return new HttpClientSettings(redirects, connectTimeout, readTimeout, sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} using defaults for all settings other than
   * the provided SSL bundle.
   *
   * @param sslBundle the SSL bundle setting
   * @return a new {@link HttpClientSettings} instance
   * @since 5.0
   */
  public static HttpClientSettings ofSslBundle(@Nullable SslBundle sslBundle) {
    return defaults().withSslBundle(sslBundle);
  }

  /**
   * Use defaults settings, which can differ depending on the implementation.
   *
   * @return default settings
   * @since 5.0
   */
  public static HttpClientSettings defaults() {
    return defaults;
  }

}
