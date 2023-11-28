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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import cn.taketoday.core.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/1 23:10
 */
class ClientHttpRequestFactorySettingsTests {

  private static final Duration ONE_SECOND = Duration.ofSeconds(1);

  @Test
  void defaultsHasNullValues() {
    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS;
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withConnectTimeoutReturnsInstanceWithUpdatedConnectionTimeout() {
    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(ONE_SECOND);
    assertThat(settings.connectTimeout()).isEqualTo(ONE_SECOND);
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withReadTimeoutReturnsInstanceWithUpdatedReadTimeout() {
    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withReadTimeout(ONE_SECOND);
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isEqualTo(ONE_SECOND);
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withBufferRequestBodyReturnsInstanceWithUpdatedBufferRequestBody() {
    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withBufferRequestBody(true);
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isTrue();
    assertThat(settings.sslBundle()).isNull();
  }

  @Test
  void withSslBundleReturnsInstanceWithUpdatedSslBundle() {
    SslBundle sslBundle = mock(SslBundle.class);
    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS.withSslBundle(sslBundle);
    assertThat(settings.connectTimeout()).isNull();
    assertThat(settings.readTimeout()).isNull();
    assertThat(settings.bufferRequestBody()).isNull();
    assertThat(settings.sslBundle()).isSameAs(sslBundle);
  }

}