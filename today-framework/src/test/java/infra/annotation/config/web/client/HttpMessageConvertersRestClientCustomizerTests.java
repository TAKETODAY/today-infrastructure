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

package infra.annotation.config.web.client;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import infra.annotation.config.http.ClientHttpMessageConvertersCustomizer;
import infra.http.converter.HttpMessageConverter;
import infra.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConvertersRestClientCustomizer}
 *
 * @author Brian Clozel
 */
class HttpMessageConvertersRestClientCustomizerTests {

  @Test
  void customizeConfiguresMessageConverters() {
    HttpMessageConverter<?> c0 = mock();
    HttpMessageConverter<?> c1 = mock();
    ClientHttpMessageConvertersCustomizer customizer = (clientBuilder) -> clientBuilder.addCustomConverter(c0).addCustomConverter(c1);

    RestClient.Builder builder = RestClient.builder();
    new HttpMessageConvertersRestClientCustomizer(customizer).customize(builder);
    assertThat(builder.build()).extracting("messageConverters")
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsSubsequence(c0, c1);
  }

}
