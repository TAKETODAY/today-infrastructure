/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.client.config;

import java.time.Duration;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.lang.Nullable;

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
public record ClientHttpRequestFactorySettings(
        @Nullable Duration connectTimeout, @Nullable Duration readTimeout,
        @Nullable Boolean bufferRequestBody, @Nullable SslBundle sslBundle) {

  /**
   * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
   * the implementation.
   */
  public static final ClientHttpRequestFactorySettings DEFAULTS = new ClientHttpRequestFactorySettings(
          null, null, null, null);

  /**
   * Create a new {@link ClientHttpRequestFactorySettings} instance.
   *
   * @param connectTimeout the connection timeout
   * @param readTimeout the read timeout
   * @param bufferRequestBody the bugger request body
   * @param sslBundle the ssl bundle
   */
  public ClientHttpRequestFactorySettings { }

  public ClientHttpRequestFactorySettings(Duration connectTimeout, Duration readTimeout, Boolean bufferRequestBody) {
    this(connectTimeout, readTimeout, bufferRequestBody, null);
  }

  /**
   * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
   * connect timeout setting .
   *
   * @param connectTimeout the new connect timeout setting
   * @return a new {@link ClientHttpRequestFactorySettings} instance
   */
  public ClientHttpRequestFactorySettings withConnectTimeout(Duration connectTimeout) {
    return new ClientHttpRequestFactorySettings(connectTimeout, this.readTimeout, this.bufferRequestBody,
            this.sslBundle);
  }

  /**
   * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated read
   * timeout setting.
   *
   * @param readTimeout the new read timeout setting
   * @return a new {@link ClientHttpRequestFactorySettings} instance
   */
  public ClientHttpRequestFactorySettings withReadTimeout(Duration readTimeout) {
    return new ClientHttpRequestFactorySettings(
            connectTimeout, readTimeout, bufferRequestBody, sslBundle);
  }

  /**
   * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
   * buffer request body setting.
   *
   * @param bufferRequestBody the new buffer request body setting
   * @return a new {@link ClientHttpRequestFactorySettings} instance
   */
  public ClientHttpRequestFactorySettings withBufferRequestBody(Boolean bufferRequestBody) {
    return new ClientHttpRequestFactorySettings(
            connectTimeout, readTimeout, bufferRequestBody, sslBundle);
  }

  /**
   * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated SSL
   * bundle setting.
   *
   * @param sslBundle the new SSL bundle setting
   * @return a new {@link ClientHttpRequestFactorySettings} instance
   */
  public ClientHttpRequestFactorySettings withSslBundle(SslBundle sslBundle) {
    return new ClientHttpRequestFactorySettings(
            connectTimeout, readTimeout, bufferRequestBody, sslBundle);
  }

}
