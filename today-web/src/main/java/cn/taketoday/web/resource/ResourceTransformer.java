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

import cn.taketoday.core.io.Resource;
import cn.taketoday.web.RequestContext;

/**
 * An abstraction for transforming the content of a resource.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface ResourceTransformer {

  /**
   * Transform the given resource.
   *
   * @param request the current request
   * @param resource the resource to transform
   * @param transformerChain the chain of remaining transformers to delegate to
   * @return the transformed resource (never {@code null})
   * @throws IOException if the transformation fails
   */
  Resource transform(RequestContext request, Resource resource, ResourceTransformerChain transformerChain)
          throws IOException;

}
