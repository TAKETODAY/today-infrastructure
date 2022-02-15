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

import cn.taketoday.core.io.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * An abstraction for transforming the content of a resource.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
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
  Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
          throws IOException;

}
