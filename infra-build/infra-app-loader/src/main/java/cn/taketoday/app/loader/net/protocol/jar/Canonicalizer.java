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

package cn.taketoday.app.loader.net.protocol.jar;

/**
 * Internal utility used by the {@link Handler} to canonicalize paths. This implementation
 * should behave the same as the canonicalization functions in
 * {@code sun.net.www.protocol.jar.Handler}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Canonicalizer {

  private Canonicalizer() {
  }

  static String canonicalizeAfter(String path, int pos) {
    int pathLength = path.length();
    boolean noDotSlash = path.indexOf("./", pos) == -1;
    if (pos >= pathLength || (noDotSlash && path.charAt(pathLength - 1) != '.')) {
      return path;
    }
    String before = path.substring(0, pos);
    String after = path.substring(pos);
    return before + canonicalize(after);
  }

  static String canonicalize(String path) {
    path = removeEmbeddedSlashDotDotSlash(path);
    path = removeEmbeddedSlashDotSlash(path);
    path = removeTrailingSlashDotDot(path);
    path = removeTrailingSlashDot(path);
    return path;
  }

  private static String removeEmbeddedSlashDotDotSlash(String path) {
    int index;
    while ((index = path.indexOf("/../")) >= 0) {
      int priorSlash = path.lastIndexOf('/', index - 1);
      String after = path.substring(index + 3);
      path = (priorSlash >= 0) ? path.substring(0, priorSlash) + after : after;
    }
    return path;
  }

  private static String removeEmbeddedSlashDotSlash(String path) {
    int index;
    while ((index = path.indexOf("/./")) >= 0) {
      String before = path.substring(0, index);
      String after = path.substring(index + 2);
      path = before + after;
    }
    return path;
  }

  private static String removeTrailingSlashDot(String path) {
    return (!path.endsWith("/.")) ? path : path.substring(0, path.length() - 1);
  }

  private static String removeTrailingSlashDotDot(String path) {
    int index;
    while (path.endsWith("/..")) {
      index = path.indexOf("/..");
      int priorSlash = path.lastIndexOf('/', index - 1);
      path = (priorSlash >= 0) ? path.substring(0, priorSlash + 1) : path.substring(0, index);
    }
    return path;
  }

}
