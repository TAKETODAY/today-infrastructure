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

package cn.taketoday.app.loader.net.protocol.nested;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.app.loader.net.util.UrlDecoder;

/**
 * A location obtained from a {@code nested:} {@link URL} consisting of a jar file and an
 * optional nested entry.
 * <p>
 * The syntax of a nested JAR URL is: <pre>
 * nestedjar:&lt;path&gt;/!{entry}
 * </pre>
 * <p>
 * for example:
 * <p>
 * {@code nested:/home/example/my.jar/!APP-INF/lib/my-nested.jar}
 * <p>
 * or:
 * <p>
 * {@code nested:/home/example/my.jar/!APP-INF/classes/}
 * <p>
 * The path must refer to a jar file on the file system. The entry refers to either an
 * uncompressed entry that contains the nested jar, or a directory entry. The entry must
 * not start with a {@code '/'}.
 *
 * @param path the path to the zip that contains the nested entry
 * @param nestedEntryName the nested entry name
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public record NestedLocation(Path path, String nestedEntryName) {

  private static final Map<String, NestedLocation> locationCache = new ConcurrentHashMap<>();

  private static final Map<String, Path> pathCache = new ConcurrentHashMap<>();

  public NestedLocation(Path path, String nestedEntryName) {
    if (path == null) {
      throw new IllegalArgumentException("'path' is required");
    }
    this.path = path;
    this.nestedEntryName = (nestedEntryName != null && !nestedEntryName.isEmpty()) ? nestedEntryName : null;
  }

  /**
   * Create a new {@link NestedLocation} from the given URL.
   *
   * @param url the nested URL
   * @return a new {@link NestedLocation} instance
   * @throws IllegalArgumentException if the URL is not valid
   */
  public static NestedLocation fromUrl(URL url) {
    if (url == null || !"nested".equalsIgnoreCase(url.getProtocol())) {
      throw new IllegalArgumentException("'url' is required and must use 'nested' protocol");
    }
    return parse(UrlDecoder.decode(url.toString().substring(7)));
  }

  /**
   * Create a new {@link NestedLocation} from the given URI.
   *
   * @param uri the nested URI
   * @return a new {@link NestedLocation} instance
   * @throws IllegalArgumentException if the URI is not valid
   */
  public static NestedLocation fromUri(URI uri) {
    if (uri == null || !"nested".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException("'uri' is required and must use 'nested' scheme");
    }
    return parse(uri.getSchemeSpecificPart());
  }

  static NestedLocation parse(String location) {
    if (location == null || location.isEmpty()) {
      throw new IllegalArgumentException("'location' must not be empty");
    }
    return locationCache.computeIfAbsent(location, (key) -> create(location));
  }

  private static NestedLocation create(String location) {
    int index = location.lastIndexOf("/!");
    String locationPath = (index != -1) ? location.substring(0, index) : location;
    String nestedEntryName = (index != -1) ? location.substring(index + 2) : null;
    return new NestedLocation((!locationPath.isEmpty()) ? asPath(locationPath) : null, nestedEntryName);
  }

  private static Path asPath(String locationPath) {
    return pathCache.computeIfAbsent(locationPath,
            (key) -> Path.of((!isWindows()) ? locationPath : fixWindowsLocationPath(locationPath)));
  }

  private static boolean isWindows() {
    return File.separatorChar == '\\';
  }

  private static String fixWindowsLocationPath(String locationPath) {
    // Same logic as Java's internal WindowsUriSupport class
    if (locationPath.length() > 2 && locationPath.charAt(2) == ':') {
      return locationPath.substring(1);
    }
    // Deal with Jetty's org.eclipse.jetty.util.URIUtil#correctURI(URI)
    if (locationPath.startsWith("///") && locationPath.charAt(4) == ':') {
      return locationPath.substring(3);
    }
    return locationPath;
  }

  static void clearCache() {
    locationCache.clear();
    pathCache.clear();
  }

}
