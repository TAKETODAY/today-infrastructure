/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web.client;

import java.util.function.Consumer;

import cn.taketoday.core.ssl.NoSuchSslBundleException;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.config.ClientHttpRequestFactories;
import cn.taketoday.web.client.config.ClientHttpRequestFactorySettings;

/**
 * Interface that can be used to {@link RestClient.Builder#apply apply} SSL configuration
 * to a {@link cn.taketoday.web.client.RestClient.Builder RestClient.Builder}.
 * <p>
 * Typically used as follows: <pre class="code">
 * &#064;Bean
 * public MyBean myBean(RestClient.Builder restClientBuilder, RestClientSsl ssl) {
 *     RestClient restClientrestClient= restClientBuilder.apply(ssl.forBundle("mybundle")).build();
 *     return new MyBean(webClient);
 * }
 * </pre> NOTE: Apply SSL configuration will replace any previously
 * {@link RestClient.Builder#requestFactory configured} {@link ClientHttpRequestFactory}.
 * If you need to configure {@link ClientHttpRequestFactory} with more than just SSL
 * consider using a {@link ClientHttpRequestFactorySettings} with
 * {@link ClientHttpRequestFactories}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RestClientSsl {

  /**
   * Return a {@link Consumer} that will apply SSL configuration for the named
   * {@link SslBundle} to a {@link cn.taketoday.web.client.RestClient.Builder
   * RestClient.Builder}.
   *
   * @param bundleName the name of the SSL bundle to apply
   * @return a {@link Consumer} to apply the configuration
   * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
   */
  Consumer<RestClient.Builder> fromBundle(String bundleName) throws NoSuchSslBundleException;

  /**
   * Return a {@link Consumer} that will apply SSL configuration for the
   * {@link SslBundle} to a {@link cn.taketoday.web.client.RestClient.Builder
   * RestClient.Builder}.
   *
   * @param bundle the SSL bundle to apply
   * @return a {@link Consumer} to apply the configuration
   */
  Consumer<RestClient.Builder> fromBundle(SslBundle bundle);

}
