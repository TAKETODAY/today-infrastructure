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
import cn.taketoday.web.RequestContext;

/**
 * A contract for invoking a chain of {@link ResourceResolver ResourceResolvers} where each resolver
 * is given a reference to the chain allowing it to delegate when necessary.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public interface ResourceResolvingChain {

  /**
   * Resolve the supplied request and request path to a {@link Resource} that
   * exists under one of the given resource locations.
   *
   * @param request the current request
   * @param requestPath the portion of the request path to use
   * @param locations the locations to search in when looking up resources
   * @return the resolved resource, or {@code null} if unresolved
   */
  @Nullable
  Resource resolveResource(
          @Nullable RequestContext request, String requestPath, List<? extends Resource> locations);

  /**
   * Resolve the externally facing <em>public</em> URL path for clients to use
   * to access the resource that is located at the given <em>internal</em>
   * resource path.
   * <p>This is useful when rendering URL links to clients.
   *
   * @param resourcePath the internal resource path
   * @param locations the locations to search in when looking up resources
   * @return the resolved public URL path, or {@code null} if unresolved
   */
  @Nullable
  String resolveUrlPath(String resourcePath, List<? extends Resource> locations);

}
