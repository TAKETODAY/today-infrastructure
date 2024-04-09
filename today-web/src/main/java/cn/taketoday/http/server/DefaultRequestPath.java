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

package cn.taketoday.http.server;

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

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
