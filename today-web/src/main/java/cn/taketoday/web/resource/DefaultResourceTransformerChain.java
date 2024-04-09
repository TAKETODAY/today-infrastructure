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

package cn.taketoday.web.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Default immutable implementation of {@link ResourceTransformerChain}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultResourceTransformerChain implements ResourceTransformerChain {

  private final ResourceResolvingChain resolverChain;

  @Nullable
  private final ResourceTransformer transformer;

  @Nullable
  private final ResourceTransformerChain nextChain;

  public DefaultResourceTransformerChain(ResourceResolvingChain resolverChain, @Nullable List<ResourceTransformer> transformers) {
    Assert.notNull(resolverChain, "ResourceResolverChain is required");
    this.resolverChain = resolverChain;
    transformers = transformers != null ? transformers : Collections.emptyList();
    DefaultResourceTransformerChain chain = initTransformerChain(resolverChain, new ArrayList<>(transformers));
    this.transformer = chain.transformer;
    this.nextChain = chain.nextChain;
  }

  private DefaultResourceTransformerChain initTransformerChain(
          ResourceResolvingChain resolverChain, ArrayList<ResourceTransformer> transformers) {
    DefaultResourceTransformerChain chain = new DefaultResourceTransformerChain(resolverChain, null, null);
    ListIterator<? extends ResourceTransformer> it = transformers.listIterator(transformers.size());
    while (it.hasPrevious()) {
      chain = new DefaultResourceTransformerChain(resolverChain, it.previous(), chain);
    }
    return chain;
  }

  public DefaultResourceTransformerChain(ResourceResolvingChain resolverChain,
          @Nullable ResourceTransformer transformer, @Nullable ResourceTransformerChain chain) {
    Assert.isTrue((transformer == null && chain == null) || (transformer != null && chain != null),
            "Both transformer and transformer chain must be null, or neither is");
    this.resolverChain = resolverChain;
    this.transformer = transformer;
    this.nextChain = chain;
  }

  @Override
  public ResourceResolvingChain getResolvingChain() {
    return this.resolverChain;
  }

  @Override
  public Resource transform(RequestContext request, Resource resource) throws IOException {
    return transformer != null && nextChain != null
            ? transformer.transform(request, resource, nextChain) : resource;
  }

}
