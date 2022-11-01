/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import cn.taketoday.http.client.ClientHttpRequestFactory;

/**
 * Settings that can be applied when creating a {@link ClientHttpRequestFactory}.
 *
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param bufferRequestBody if request body buffering is used
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClientHttpRequestFactories
 * @since 4.0
 */
public record ClientHttpRequestFactorySettings(
        Duration connectTimeout, Duration readTimeout, Boolean bufferRequestBody) {

  /**
   * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
   * the implementation.
   */
  public static final ClientHttpRequestFactorySettings DEFAULTS
          = new ClientHttpRequestFactorySettings(null, null, null);

  /**
   * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
   * connect timeout setting .
   *
   * @param connectTimeout the new connect timeout setting
   * @return a new {@link ClientHttpRequestFactorySettings} instance
   */
  public ClientHttpRequestFactorySettings withConnectTimeout(Duration connectTimeout) {
    return new ClientHttpRequestFactorySettings(connectTimeout, readTimeout, bufferRequestBody);
  }

  /**
   * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated read
   * timeout setting.
   *
   * @param readTimeout the new read timeout setting
   * @return a new {@link ClientHttpRequestFactorySettings} instance
   */
  public ClientHttpRequestFactorySettings withReadTimeout(Duration readTimeout) {
    return new ClientHttpRequestFactorySettings(connectTimeout, readTimeout, bufferRequestBody);
  }

  /**
   * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
   * buffer request body setting.
   *
   * @param bufferRequestBody the new buffer request body setting
   * @return a new {@link ClientHttpRequestFactorySettings} instance
   */
  public ClientHttpRequestFactorySettings withBufferRequestBody(Boolean bufferRequestBody) {
    return new ClientHttpRequestFactorySettings(connectTimeout, readTimeout, bufferRequestBody);
  }

}
