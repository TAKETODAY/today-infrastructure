/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Default immutable implementation of {@link ResourceTransformerChain}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultResourceTransformerChain implements ResourceTransformerChain {

  private final ResourceResolverChain resolverChain;

  @Nullable
  private final ResourceTransformer transformer;

  @Nullable
  private final ResourceTransformerChain nextChain;

  public DefaultResourceTransformerChain(
          ResourceResolverChain resolverChain, @Nullable List<ResourceTransformer> transformers) {

    Assert.notNull(resolverChain, "ResourceResolverChain is required");
    this.resolverChain = resolverChain;
    transformers = (transformers != null ? transformers : Collections.emptyList());
    DefaultResourceTransformerChain chain = initTransformerChain(resolverChain, new ArrayList<>(transformers));
    this.transformer = chain.transformer;
    this.nextChain = chain.nextChain;
  }

  private DefaultResourceTransformerChain initTransformerChain(ResourceResolverChain resolverChain,
                                                               ArrayList<ResourceTransformer> transformers) {

    DefaultResourceTransformerChain chain = new DefaultResourceTransformerChain(resolverChain, null, null);
    ListIterator<? extends ResourceTransformer> it = transformers.listIterator(transformers.size());
    while (it.hasPrevious()) {
      chain = new DefaultResourceTransformerChain(resolverChain, it.previous(), chain);
    }
    return chain;
  }

  public DefaultResourceTransformerChain(ResourceResolverChain resolverChain,
                                         @Nullable ResourceTransformer transformer, @Nullable ResourceTransformerChain chain) {

    Assert.isTrue((transformer == null && chain == null) || (transformer != null && chain != null),
            "Both transformer and transformer chain must be null, or neither is");

    this.resolverChain = resolverChain;
    this.transformer = transformer;
    this.nextChain = chain;
  }

  @Override
  public ResourceResolverChain getResolverChain() {
    return this.resolverChain;
  }

  @Override
  public Resource transform(HttpServletRequest request, Resource resource) throws IOException {
    return (this.transformer != null && this.nextChain != null ?
            this.transformer.transform(request, resource, this.nextChain) : resource);
  }

}
