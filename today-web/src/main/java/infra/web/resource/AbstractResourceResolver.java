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

package infra.web.resource;

import java.util.List;

import infra.core.io.Resource;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.RequestContext;

/**
 * Base class for {@link infra.web.resource.ResourceResolver}
 * implementations. Provides consistent logging.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  @Nullable
  public Resource resolveResource(@Nullable RequestContext request, String requestPath,
          List<? extends Resource> locations, ResourceResolvingChain chain) {
    return resolveResourceInternal(request, requestPath, locations, chain);
  }

  @Override
  @Nullable
  public String resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations, ResourceResolvingChain chain) {
    return resolveUrlPathInternal(resourceUrlPath, locations, chain);
  }

  @Nullable
  protected abstract Resource resolveResourceInternal(@Nullable RequestContext request,
          String requestPath, List<? extends Resource> locations, ResourceResolvingChain chain);

  @Nullable
  protected abstract String resolveUrlPathInternal(
          String resourceUrlPath, List<? extends Resource> locations, ResourceResolvingChain chain);

}
