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

import java.util.List;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Base class for {@link cn.taketoday.web.resource.ResourceResolver}
 * implementations. Provides consistent logging.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  @Nullable
  public Resource resolveResource(
          @Nullable RequestContext request, String requestPath,
          List<? extends Resource> locations, ResourceResolvingChain chain) {
    return resolveResourceInternal(request, requestPath, locations, chain);
  }

  @Override
  @Nullable
  public String resolveUrlPath(
          String resourceUrlPath, List<? extends Resource> locations, ResourceResolvingChain chain) {

    return resolveUrlPathInternal(resourceUrlPath, locations, chain);
  }

  @Nullable
  protected abstract Resource resolveResourceInternal(
          @Nullable RequestContext request,
          String requestPath, List<? extends Resource> locations, ResourceResolvingChain chain);

  @Nullable
  protected abstract String resolveUrlPathInternal(
          String resourceUrlPath, List<? extends Resource> locations, ResourceResolvingChain chain);

}
