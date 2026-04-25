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

package infra.app.resttestclient.config;

import java.util.Collection;

import infra.http.converter.config.ClientHttpMessageConvertersCustomizer;
import infra.test.web.mock.client.RestTestClient;

/**
 * {@link RestTestClientBuilderCustomizer} for a typical Spring Boot application. Usually
 * applied automatically via
 * {@link AutoConfigureRestTestClient @AutoConfigureRestTestClient}, but may also be used
 * directly.
 *
 * @author Andy Wilkinson
 * @since 5.0
 */
public class InfraRestTestClientBuilderCustomizer implements RestTestClientBuilderCustomizer {

  private final Collection<ClientHttpMessageConvertersCustomizer> messageConverterCustomizers;

  /**
   * Create a new {@code SpringBootRestTestClientBuilderCustomizer} that will configure
   * the builder's message converters using the given
   * {@code messageConverterCustomizers}.
   *
   * @param messageConverterCustomizers the message converter customizers
   */
  public InfraRestTestClientBuilderCustomizer(
          Collection<ClientHttpMessageConvertersCustomizer> messageConverterCustomizers) {
    this.messageConverterCustomizers = messageConverterCustomizers;

  }

  @Override
  public void customize(RestTestClient.Builder<?> builder) {
    if (this.messageConverterCustomizers.isEmpty()) {
      return;
    }
    builder.configureMessageConverters((configurer) -> this.messageConverterCustomizers
            .forEach((customizer) -> customizer.customize(configurer)));
  }

}
