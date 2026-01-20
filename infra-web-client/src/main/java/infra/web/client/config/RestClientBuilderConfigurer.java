/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.client.config;

import java.util.List;

import infra.http.client.ClientHttpRequestFactoryBuilder;
import infra.http.client.HttpClientSettings;
import infra.web.client.RestClient;
import infra.web.client.RestClientCustomizer;

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
