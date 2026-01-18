/*
 * Copyright 2002-present the original author or authors.
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

package infra.http.service.support;

import infra.http.service.invoker.HttpExchangeAdapter;
import infra.http.service.registry.HttpServiceGroupAdapter;
import infra.http.service.registry.HttpServiceGroupConfigurer;
import infra.web.reactive.client.WebClient;

/**
 * Adapter for groups backed by {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@SuppressWarnings("unused")
public class WebClientHttpServiceGroupAdapter implements HttpServiceGroupAdapter<WebClient.Builder> {

  @Override
  public WebClient.Builder createClientBuilder() {
    return WebClient.builder();
  }

  @Override
  public Class<? extends HttpServiceGroupConfigurer<WebClient.Builder>> getConfigurerType() {
    return WebClientHttpServiceGroupConfigurer.class;
  }

  @Override
  public HttpExchangeAdapter createExchangeAdapter(WebClient.Builder clientBuilder) {
    return WebClientAdapter.forClient(clientBuilder.build());
  }

}
