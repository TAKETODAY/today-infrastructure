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

package infra.web.resource;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import infra.core.io.Resource;
import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * Default immutable implementation of {@link ResourceResolvingChain}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultResourceResolvingChain implements ResourceResolvingChain {

  @Nullable
  private final ResourceResolver resolver;

  @Nullable
  private final ResourceResolvingChain nextChain;

  public DefaultResourceResolvingChain(@Nullable List<? extends ResourceResolver> resolvers) {
    if (resolvers == null) {
      resolvers = Collections.emptyList();
    }
    DefaultResourceResolvingChain chain = initChain(new ArrayList<>(resolvers));
    this.resolver = chain.resolver;
    this.nextChain = chain.nextChain;
  }

  private static DefaultResourceResolvingChain initChain(ArrayList<? extends ResourceResolver> resolvers) {
    DefaultResourceResolvingChain chain = new DefaultResourceResolvingChain(null, null);
    ListIterator<? extends ResourceResolver> it = resolvers.listIterator(resolvers.size());
    while (it.hasPrevious()) {
      chain = new DefaultResourceResolvingChain(it.previous(), chain);
    }
    return chain;
  }

  private DefaultResourceResolvingChain(@Nullable ResourceResolver resolver, @Nullable ResourceResolvingChain chain) {
    Assert.isTrue((resolver == null && chain == null) || (resolver != null && chain != null),
            "Both resolver and resolver chain must be null, or neither is");
    this.resolver = resolver;
    this.nextChain = chain;
  }

  @Override
  @Nullable
  public Resource resolveResource(
          @Nullable RequestContext request, String requestPath, List<? extends Resource> locations) {
    return resolver != null && nextChain != null
            ? resolver.resolveResource(request, requestPath, locations, nextChain) : null;
  }

  @Override
  @Nullable
  public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
    return resolver != null && nextChain != null ?
            resolver.resolveUrlPath(resourcePath, locations, nextChain) : null;
  }

}
