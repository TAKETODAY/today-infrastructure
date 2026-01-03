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

import java.util.List;

import infra.web.client.RestClient;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.web.client.config.RestClientCustomizer;

/**
 * Configure {@link RestClient.Builder RestClient.Builder} with sensible defaults.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RestClient.Builder} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class RestClientBuilderConfigurer {

  private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

  private final HttpClientSettings clientSettings;

  private final List<RestClientCustomizer> customizers;

  public RestClientBuilderConfigurer(ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
          HttpClientSettings clientSettings, List<RestClientCustomizer> customizers) {
    this.requestFactoryBuilder = requestFactoryBuilder;
    this.clientSettings = clientSettings;
    this.customizers = customizers;
  }

  /**
   * Configure the specified {@link RestClient.Builder RestClient.Builder}. The builder can be
   * further tuned and default settings can be overridden.
   *
   * @param builder the {@link RestClient.Builder RestClient.Builder} instance to configure
   * @return the configured builder
   */
  public RestClient.Builder configure(RestClient.Builder builder) {
    builder.requestFactory(this.requestFactoryBuilder.build(this.clientSettings));
    applyCustomizers(builder);
    return builder;
  }

  private void applyCustomizers(RestClient.Builder builder) {
    for (RestClientCustomizer customizer : this.customizers) {
      customizer.customize(builder);
    }
  }

}
