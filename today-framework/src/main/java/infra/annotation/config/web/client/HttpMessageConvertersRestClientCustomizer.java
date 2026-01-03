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

import java.util.Arrays;
import java.util.List;

import infra.http.converter.HttpMessageConverter;
import infra.annotation.config.http.ClientHttpMessageConvertersCustomizer;
import infra.lang.Assert;
import infra.web.client.RestClient;
import infra.web.client.config.RestClientCustomizer;

/**
 * {@link RestClientCustomizer} to apply {@link HttpMessageConverter
 * HttpMessageConverters}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpMessageConvertersRestClientCustomizer implements RestClientCustomizer {

  private final List<ClientHttpMessageConvertersCustomizer> customizers;

  public HttpMessageConvertersRestClientCustomizer(ClientHttpMessageConvertersCustomizer... customizers) {
    this(Arrays.asList(customizers));
  }

  public HttpMessageConvertersRestClientCustomizer(List<ClientHttpMessageConvertersCustomizer> customizers) {
    Assert.notNull(customizers, "customizers is required");
    this.customizers = customizers;
  }

  @Override
  public void customize(RestClient.Builder restClientBuilder) {
    restClientBuilder.configureMessageConverters(builder -> {
      for (var customizer : customizers) {
        customizer.customize(builder);
      }
    });
  }

}
