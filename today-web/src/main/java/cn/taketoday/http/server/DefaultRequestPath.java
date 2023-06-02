/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.server;

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Default implementation of {@link RequestPath}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultRequestPath extends RequestPath {

  private final PathContainer fullPath;
  private final PathContainer contextPath;
  private final PathContainer pathWithinApplication;

  DefaultRequestPath(String rawPath, @Nullable String contextPath) {
    this.fullPath = PathContainer.parsePath(rawPath);
    this.contextPath = initContextPath(this.fullPath, contextPath);
    this.pathWithinApplication = extractPathWithinApplication(this.fullPath, this.contextPath);
  }

  private DefaultRequestPath(RequestPath requestPath, String contextPath) {
    this.fullPath = requestPath;
    this.contextPath = initContextPath(this.fullPath, contextPath);
    this.pathWithinApplication = extractPathWithinApplication(this.fullPath, this.contextPath);
  }

  private static PathContainer initContextPath(PathContainer path, @Nullable String contextPath) {
    if (StringUtils.isBlank(contextPath) || StringUtils.matchesCharacter(contextPath, '/')) {
      return PathContainer.parsePath("");
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
    throw new IllegalStateException(
            "Failed to initialize contextPath '" + contextPath + "'" +
                    " for requestPath '" + path.value() + "'");
  }

  private static void validateContextPath(String fullPath, String contextPath) {
    int length = contextPath.length();
    if (contextPath.charAt(0) != '/' || contextPath.charAt(length - 1) == '/') {
      throw new IllegalArgumentException(
              "Invalid contextPath: '" + contextPath + "': " + "must start with '/' and not end with '/'");
    }
    if (!fullPath.startsWith(contextPath)) {
      throw new IllegalArgumentException(
              "Invalid contextPath '" + contextPath + "': " +
                      "must match the start of requestPath: '" + fullPath + "'");
    }
    if (fullPath.length() > length && fullPath.charAt(length) != '/') {
      throw new IllegalArgumentException(
              "Invalid contextPath '" + contextPath + "': " +
                      "must match to full path segments for requestPath: '" + fullPath + "'");
    }
  }

  private static PathContainer extractPathWithinApplication(PathContainer fullPath, PathContainer contextPath) {
    return fullPath.subPath(contextPath.elements().size());
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
  public RequestPath modifyContextPath(String contextPath) {
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
