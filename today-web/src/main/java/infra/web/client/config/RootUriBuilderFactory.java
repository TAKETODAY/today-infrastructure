/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.client.config;

import infra.lang.Assert;
import infra.web.client.RestTemplate;
import infra.web.util.UriBuilder;
import infra.web.util.UriBuilderFactory;
import infra.web.util.UriComponentsBuilder;
import infra.web.util.UriTemplateHandler;

/**
 * {@link UriBuilderFactory} to set the root for URI that starts with {@code '/'}.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RootUriBuilderFactory extends RootUriTemplateHandler implements UriBuilderFactory {

  RootUriBuilderFactory(String rootUri) {
    super(rootUri);
  }

  RootUriBuilderFactory(String rootUri, UriTemplateHandler delegate) {
    super(rootUri, delegate);
  }

  @Override
  public UriBuilder uriString(String uriTemplate) {
    return UriComponentsBuilder.forURIString(apply(uriTemplate));
  }

  @Override
  public UriBuilder builder() {
    return UriComponentsBuilder.create();
  }

  /**
   * Apply a {@link RootUriBuilderFactory} instance to the given {@link RestTemplate}.
   *
   * @param restTemplate the {@link RestTemplate} to add the builder factory to
   * @param rootUri the root URI
   */
  static void applyTo(RestTemplate restTemplate, String rootUri) {
    Assert.notNull(restTemplate, "RestTemplate is required");
    RootUriBuilderFactory handler = new RootUriBuilderFactory(rootUri, restTemplate.getUriTemplateHandler());
    restTemplate.setUriTemplateHandler(handler);
  }

}
