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

package infra.origin;

import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * Simple class that understands Jar URLs can can provide short descriptions.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @since 4.0
 */
final class JarUri {

  private static final String JAR_SCHEME = "jar:";

  private static final String JAR_EXTENSION = ".jar";

  private final String uri;

  private final String description;

  private JarUri(String uri) {
    this.uri = uri;
    this.description = extractDescription(uri);
  }

  private String extractDescription(String uri) {
    uri = uri.substring(JAR_SCHEME.length());
    int firstDotJar = uri.indexOf(JAR_EXTENSION);
    String firstJar = getFilename(uri.substring(0, firstDotJar + JAR_EXTENSION.length()));
    uri = uri.substring(firstDotJar + JAR_EXTENSION.length());
    int lastDotJar = uri.lastIndexOf(JAR_EXTENSION);
    if (lastDotJar == -1) {
      return firstJar;
    }
    return firstJar + uri.substring(0, lastDotJar + JAR_EXTENSION.length());
  }

  private String getFilename(String string) {
    int lastSlash = string.lastIndexOf('/');
    return (lastSlash == -1) ? string : string.substring(lastSlash + 1);
  }

  String getDescription() {
    return this.description;
  }

  String getDescription(String existing) {
    return existing + " from " + this.description;
  }

  @Override
  public String toString() {
    return this.uri;
  }

  @Nullable
  static JarUri from(URI uri) {
    return from(uri.toString());
  }

  @Nullable
  static JarUri from(String uri) {
    if (uri.startsWith(JAR_SCHEME) && uri.contains(JAR_EXTENSION)) {
      return new JarUri(uri);
    }
    return null;
  }

}
