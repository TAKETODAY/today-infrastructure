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

package infra.web.accept;

import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

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

  private final @Nullable Predicate<RequestPath> versionPathPredicate;

  /**
   * Create a resolver instance.
   *
   * @param pathSegmentIndex the index of the path segment that contains
   * the API version
   */
  public PathApiVersionResolver(int pathSegmentIndex) {
    this(pathSegmentIndex, null);
  }

  /**
   * Constructor variant of {@link #PathApiVersionResolver(int)} with an
   * additional {@code Predicate<RequestPath>} to help determine whether
   * a given path is versioned (true) or not (false).
   */
  public PathApiVersionResolver(int pathSegmentIndex, @Nullable Predicate<RequestPath> versionPathPredicate) {
    Assert.isTrue(pathSegmentIndex >= 0, "'pathSegmentIndex' must be >= 0");
    this.pathSegmentIndex = pathSegmentIndex;
    this.versionPathPredicate = versionPathPredicate;
  }

  @Override
  public @Nullable String resolveVersion(RequestContext request) {
    RequestPath path = request.getRequestPath();
    if (!isVersionedPath(path)) {
      return null;
    }
    int i = 0;
    for (PathContainer.Element element : path.pathWithinApplication().elements()) {
      if (element instanceof PathContainer.PathSegment && i++ == this.pathSegmentIndex) {
        return element.value();
      }
    }
    throw new InvalidApiVersionException("No path segment at index " + this.pathSegmentIndex);
  }

  private boolean isVersionedPath(RequestPath path) {
    return versionPathPredicate == null || versionPathPredicate.test(path);
  }

}
