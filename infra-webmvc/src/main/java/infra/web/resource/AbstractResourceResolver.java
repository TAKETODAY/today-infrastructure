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

import java.util.List;

import infra.core.io.Resource;
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
