/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation.config.web.reactive.client;

import java.util.function.Consumer;

import infra.core.ssl.NoSuchSslBundleException;
import infra.core.ssl.SslBundle;
import infra.http.client.reactive.ClientHttpConnector;
import infra.web.client.reactive.WebClient;

/**
 * Interface that can be used to {@link WebClient.Builder#apply apply} SSL configuration
 * to a {@link infra.web.client.reactive.WebClient.Builder
 * WebClient.Builder}.
 * <p>
 * Typically used as follows: <pre class="code">
 * &#064;Bean
 * public MyBean myBean(WebClient.Builder webClientBuilder, WebClientSsl ssl) {
 *     WebClient webClient = webClientBuilder.apply(ssl.forBundle("mybundle")).build();
 *     return new MyBean(webClient);
 * }
 * </pre> NOTE: Apply SSL configuration will replace any previously
 * {@link WebClient.Builder#clientConnector configured} {@link ClientHttpConnector}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface WebClientSsl {

  /**
   * Return a {@link Consumer} that will apply SSL configuration for the named
   * {@link SslBundle} to a
   * {@link infra.web.client.reactive.WebClient.Builder
   * WebClient.Builder}.
   *
   * @param bundleName the name of the SSL bundle to apply
   * @return a {@link Consumer} to apply the configuration
   * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
   */
  Consumer<WebClient.Builder> fromBundle(String bundleName) throws NoSuchSslBundleException;

  /**
   * Return a {@link Consumer} that will apply SSL configuration for the
   * {@link SslBundle} to a
   * {@link infra.web.client.reactive.WebClient.Builder
   * WebClient.Builder}.
   *
   * @param bundle the SSL bundle to apply
   * @return a {@link Consumer} to apply the configuration
   */
  Consumer<WebClient.Builder> fromBundle(SslBundle bundle);

}
