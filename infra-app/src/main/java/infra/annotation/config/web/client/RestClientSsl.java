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

import java.util.function.Consumer;

import infra.core.ssl.NoSuchSslBundleException;
import infra.core.ssl.SslBundle;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.web.client.RestClient;

/**
 * Interface that can be used to {@link RestClient.Builder#apply apply} SSL configuration
 * to a {@link infra.web.client.RestClient.Builder RestClient.Builder}.
 * <p>
 * Typically used as follows: <pre class="code">
 * &#064;Bean
 * public MyBean myBean(RestClient.Builder restClientBuilder, RestClientSsl ssl) {
 *     RestClient restClientrestClient= restClientBuilder.apply(ssl.forBundle("mybundle")).build();
 *     return new MyBean(webClient);
 * }
 * </pre> NOTE: Applying SSL configuration will replace any previously
 * {@link RestClient.Builder#requestFactory configured} {@link ClientHttpRequestFactory}.
 * The replacement {@link ClientHttpRequestFactory} will apply only configured
 * {@link HttpClientSettings} and the appropriate {@link SslBundle}.
 * <p>
 * If you need to configure {@link ClientHttpRequestFactory} with more than just SSL
 * consider using a {@link ClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RestClientSsl {

  /**
   * Return a {@link Consumer} that will apply SSL configuration for the named
   * {@link SslBundle} to a {@link infra.web.client.RestClient.Builder
   * RestClient.Builder}.
   *
   * @param bundleName the name of the SSL bundle to apply
   * @return a {@link Consumer} to apply the configuration
   * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
   */
  Consumer<RestClient.Builder> fromBundle(String bundleName) throws NoSuchSslBundleException;

  /**
   * Return a {@link Consumer} that will apply SSL configuration for the
   * {@link SslBundle} to a {@link infra.web.client.RestClient.Builder
   * RestClient.Builder}.
   *
   * @param bundle the SSL bundle to apply
   * @return a {@link Consumer} to apply the configuration
   */
  Consumer<RestClient.Builder> fromBundle(SslBundle bundle);

}
