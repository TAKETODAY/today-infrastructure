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

package cn.taketoday.web.client.config;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.util.UriBuilder;
import cn.taketoday.web.util.UriBuilderFactory;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.UriTemplateHandler;

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
    return UriComponentsBuilder.fromUriString(apply(uriTemplate));
  }

  @Override
  public UriBuilder builder() {
    return UriComponentsBuilder.newInstance();
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
