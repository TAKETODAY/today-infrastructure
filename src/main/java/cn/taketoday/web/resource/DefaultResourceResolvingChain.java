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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Default immutable implementation of {@link ResourceResolvingChain}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultResourceResolvingChain implements ResourceResolvingChain {

  @Nullable
  private final ResourceResolver resolver;

  @Nullable
  private final ResourceResolvingChain nextChain;

  public DefaultResourceResolvingChain(@Nullable List<? extends ResourceResolver> resolvers) {
    resolvers = resolvers != null ? resolvers : Collections.emptyList();
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
