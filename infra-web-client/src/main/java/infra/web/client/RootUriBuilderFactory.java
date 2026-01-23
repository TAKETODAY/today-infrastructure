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

package infra.web.client;

import infra.lang.Assert;
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
