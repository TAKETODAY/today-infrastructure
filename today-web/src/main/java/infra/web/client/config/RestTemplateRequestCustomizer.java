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

import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestInitializer;
import infra.web.client.RestTemplate;

/**
 * Callback interface that can be used to customize the {@link ClientHttpRequest} sent
 * from a {@link RestTemplate}.
 *
 * @param <T> the {@link ClientHttpRequest} type
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RestTemplateBuilder
 * @see ClientHttpRequestInitializer
 * @since 4.0
 */
@FunctionalInterface
public interface RestTemplateRequestCustomizer<T extends ClientHttpRequest> {

  /**
   * Customize the specified {@link ClientHttpRequest}.
   *
   * @param request the request to customize
   */
  void customize(T request);

}
