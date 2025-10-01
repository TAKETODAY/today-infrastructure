/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.core.io.Resource;
import infra.web.RequestContext;

/**
 * A strategy for resolving a request to a server-side resource.
 *
 * <p>Provides mechanisms for resolving an incoming request to an actual
 * {@link Resource} and for obtaining the
 * public URL path that clients should use when requesting the resource.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.web.resource.ResourceResolvingChain
 * @since 4.0
 */
public interface ResourceResolver {

  /**
   * Resolve the supplied request and request path to a {@link Resource} that
   * exists under one of the given resource locations.
   *
   * @param request the current request (may not be present in some calls)
   * @param requestPath the portion of the request path to use
   * @param locations the locations to search in when looking up resources
   * @param chain the chain of remaining resolvers to delegate to
   * @return the resolved resource, or {@code null} if unresolved
   */
  @Nullable
  Resource resolveResource(@Nullable RequestContext request, String requestPath,
          List<? extends Resource> locations, ResourceResolvingChain chain);

  /**
   * Resolve the externally facing <em>public</em> URL path for clients to use
   * to access the resource that is located at the given <em>internal</em>
   * resource path.
   * <p>This is useful when rendering URL links to clients.
   *
   * @param resourcePath the internal resource path
   * @param locations the locations to search in when looking up resources
   * @param chain the chain of resolvers to delegate to
   * @return the resolved public URL path, or {@code null} if unresolved
   */
  @Nullable
  String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolvingChain chain);

}
