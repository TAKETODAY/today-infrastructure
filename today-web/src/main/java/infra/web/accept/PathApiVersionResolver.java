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

package infra.web.accept;



import infra.http.server.PathContainer;
import infra.http.server.RequestPath;
import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * {@link ApiVersionResolver} that extract the version from a path segment.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class PathApiVersionResolver implements ApiVersionResolver {

  private final int pathSegmentIndex;

  /**
   * Create a resolver instance.
   *
   * @param pathSegmentIndex the index of the path segment that contains
   * the API version
   */
  public PathApiVersionResolver(int pathSegmentIndex) {
    Assert.isTrue(pathSegmentIndex >= 0, "'pathSegmentIndex' must be >= 0");
    this.pathSegmentIndex = pathSegmentIndex;
  }

  @Nullable
  @Override
  public String resolveVersion(RequestContext request) {
    RequestPath path = request.getRequestPath();
    int i = 0;
    for (PathContainer.Element e : path.pathWithinApplication().elements()) {
      if (e instanceof PathContainer.PathSegment && i++ == this.pathSegmentIndex) {
        return e.value();
      }
    }
    return null;
  }

}
