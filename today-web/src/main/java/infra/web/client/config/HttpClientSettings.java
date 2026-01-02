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

import org.jspecify.annotations.Nullable;

import java.time.Duration;

import infra.core.ssl.SslBundle;
import infra.http.client.ClientHttpRequestFactory;

/**
 * Settings that can be applied when creating a {@link ClientHttpRequestFactory}.
 *
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param bufferRequestBody if request body buffering is used
 * @param sslBundle the SSL bundle providing SSL configuration
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClientHttpRequestFactories
 * @since 4.0
 */
public record HttpClientSettings(
        @Nullable Duration connectTimeout, @Nullable Duration readTimeout,
        @Nullable Boolean bufferRequestBody, @Nullable SslBundle sslBundle) {

  /**
   * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
   * the implementation.
   */
  public static final HttpClientSettings DEFAULTS = new HttpClientSettings(
          null, null, null, null);

  /**
   * Create a new {@link HttpClientSettings} instance.
   *
   * @param connectTimeout the connection timeout
   * @param readTimeout the read timeout
   * @param bufferRequestBody the bugger request body
   * @param sslBundle the ssl bundle
   */
  public HttpClientSettings {
  }

  public HttpClientSettings(Duration connectTimeout, Duration readTimeout, Boolean bufferRequestBody) {
    this(connectTimeout, readTimeout, bufferRequestBody, null);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated
   * connect timeout setting .
   *
   * @param connectTimeout the new connect timeout setting
   * @return a new {@link HttpClientSettings} instance
   */
  public HttpClientSettings withConnectTimeout(Duration connectTimeout) {
    return new HttpClientSettings(connectTimeout, this.readTimeout, this.bufferRequestBody,
            this.sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated read
   * timeout setting.
   *
   * @param readTimeout the new read timeout setting
   * @return a new {@link HttpClientSettings} instance
   */
  public HttpClientSettings withReadTimeout(Duration readTimeout) {
    return new HttpClientSettings(
            connectTimeout, readTimeout, bufferRequestBody, sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated
   * buffer request body setting.
   *
   * @param bufferRequestBody the new buffer request body setting
   * @return a new {@link HttpClientSettings} instance
   */
  public HttpClientSettings withBufferRequestBody(Boolean bufferRequestBody) {
    return new HttpClientSettings(
            connectTimeout, readTimeout, bufferRequestBody, sslBundle);
  }

  /**
   * Return a new {@link HttpClientSettings} instance with an updated SSL
   * bundle setting.
   *
   * @param sslBundle the new SSL bundle setting
   * @return a new {@link HttpClientSettings} instance
   */
  public HttpClientSettings withSslBundle(SslBundle sslBundle) {
    return new HttpClientSettings(
            connectTimeout, readTimeout, bufferRequestBody, sslBundle);
  }

}
