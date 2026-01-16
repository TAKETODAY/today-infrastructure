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

package infra.http.server;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.util.StringUtils;

/**
 * Default implementation of {@link RequestPath}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultRequestPath extends RequestPath {

  private final PathContainer fullPath;

  private final PathContainer contextPath;

  private final PathContainer pathWithinApplication;

  DefaultRequestPath(String rawPath, @Nullable String contextPath) {
    this(PathContainer.parsePath(rawPath), contextPath);
  }

  private DefaultRequestPath(PathContainer fullPath, @Nullable String contextPath) {
    this.fullPath = fullPath;
    this.contextPath = initContextPath(fullPath, contextPath);
    this.pathWithinApplication =
            contextPath == null ? fullPath : fullPath.subPath(this.contextPath.elements().size());
  }

  private static PathContainer initContextPath(PathContainer path, @Nullable String contextPath) {
    if (contextPath == null
            || StringUtils.isBlank(contextPath)
            || StringUtils.matchesCharacter(contextPath, '/')) {
      return PathContainer.empty();
    }

    validateContextPath(path.value(), contextPath);
    int length = contextPath.length();
    int counter = 0;

    List<Element> elements = path.elements();
    int size = elements.size();
    for (int i = 0; i < size; i++) {
      Element element = elements.get(i);
      counter += element.value().length();
      if (length == counter) {
        return path.subPath(0, i + 1);
      }
    }

    // Should not happen..
    throw new IllegalStateException("Failed to initialize contextPath '%s' for requestPath '%s'"
            .formatted(contextPath, path.value()));
  }

  private static void validateContextPath(String fullPath, String contextPath) {
    int length = contextPath.length();
    if (contextPath.charAt(0) != '/' || contextPath.charAt(length - 1) == '/') {
      throw new IllegalArgumentException(
              "Invalid contextPath: '%s': must start with '/' and not end with '/'".formatted(contextPath));
    }
    if (!fullPath.startsWith(contextPath)) {
      throw new IllegalArgumentException("Invalid contextPath '%s': must match the start of requestPath: '%s'"
              .formatted(contextPath, fullPath));
    }
    if (fullPath.length() > length && fullPath.charAt(length) != '/') {
      throw new IllegalArgumentException("Invalid contextPath '%s': must match to full path segments for requestPath: '%s'"
              .formatted(contextPath, fullPath));
    }
  }

  // PathContainer methods..

  @Override
  public String value() {
    return this.fullPath.value();
  }

  @Override
  public List<Element> elements() {
    return this.fullPath.elements();
  }

  // RequestPath methods..

  @Override
  public PathContainer contextPath() {
    return this.contextPath;
  }

  @Override
  public PathContainer pathWithinApplication() {
    return this.pathWithinApplication;
  }

  @Override
  public RequestPath modifyContextPath(@Nullable String contextPath) {
    return new DefaultRequestPath(this, contextPath);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    DefaultRequestPath otherPath = (DefaultRequestPath) other;
    return (this.fullPath.equals(otherPath.fullPath) &&
            this.contextPath.equals(otherPath.contextPath) &&
            this.pathWithinApplication.equals(otherPath.pathWithinApplication));
  }

  @Override
  public int hashCode() {
    int result = this.fullPath.hashCode();
    result = 31 * result + this.contextPath.hashCode();
    result = 31 * result + this.pathWithinApplication.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return this.fullPath.toString();
  }

}
