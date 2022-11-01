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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import cn.taketoday.http.client.ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base classes for testing of {@link ClientHttpRequestFactories} with different HTTP
 * clients on the classpath.
 *
 * @param <T> the {@link ClientHttpRequestFactory} to be produced
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/1 23:10
 */
abstract class AbstractClientHttpRequestFactoriesTests<T extends ClientHttpRequestFactory> {

  private final Class<T> requestFactoryType;

  protected AbstractClientHttpRequestFactoriesTests(Class<T> requestFactoryType) {
    this.requestFactoryType = requestFactoryType;
  }

  @Test
  void getReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
  }

  @Test
  void getOfGeneralTypeReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
  }

  @Test
  void getOfSpecificTypeReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(this.requestFactoryType,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getReturnsRequestFactoryWithConfiguredConnectTimeout() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(60)));
    assertThat(connectTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(60).toMillis());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getReturnsRequestFactoryWithConfiguredReadTimeout() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(120)));
    assertThat(readTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(120).toMillis());
  }

  protected abstract long connectTimeout(T requestFactory);

  protected abstract long readTimeout(T requestFactory);

}

