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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;

/**
 * Utility class with factory methods that can be used to create JAR URLs.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class JarUrl {

  private JarUrl() {
  }

  /**
   * Create a new jar URL.
   *
   * @param file the jar file
   * @return a jar file URL
   */
  public static URL create(File file) {
    return create(file, (String) null);
  }

  /**
   * Create a new jar URL.
   *
   * @param file the jar file
   * @param nestedEntry the nested entry or {@code null}
   * @return a jar file URL
   */
  public static URL create(File file, JarEntry nestedEntry) {
    return create(file, (nestedEntry != null) ? nestedEntry.getName() : null);
  }

  /**
   * Create a new jar URL.
   *
   * @param file the jar file
   * @param nestedEntryName the nested entry name or {@code null}
   * @return a jar file URL
   */
  public static URL create(File file, String nestedEntryName) {
    return create(file, nestedEntryName, null);
  }

  /**
   * Create a new jar URL.
   *
   * @param file the jar file
   * @param nestedEntryName the nested entry name or {@code null}
   * @param path the path within the jar or nested jar
   * @return a jar file URL
   */
  public static URL create(File file, String nestedEntryName, String path) {
    try {
      path = (path != null) ? path : "";
      return new URL(null, "jar:" + getJarReference(file, nestedEntryName) + "!/" + path, Handler.INSTANCE);
    }
    catch (MalformedURLException ex) {
      throw new IllegalStateException("Unable to create JarFileArchive URL", ex);
    }
  }

  private static String getJarReference(File file, String nestedEntryName) {
    String jarFilePath = file.toURI().getRawPath().replace("!", "%21");
    return (nestedEntryName != null) ? "nested:" + jarFilePath + "/!" + nestedEntryName : "file:" + jarFilePath;
  }

}
