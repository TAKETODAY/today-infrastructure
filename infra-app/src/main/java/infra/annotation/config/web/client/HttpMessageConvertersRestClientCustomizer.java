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
