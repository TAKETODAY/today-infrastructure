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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} to support {@code nested:} URLs. See {@link NestedLocation}
 * for details of the URL format.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class Handler extends URLStreamHandler {

  // NOTE: in order to be found as a URL protocol handler, this class must be public,
  // must be named Handler and must be in a package ending '.nested'

  private static final String PREFIX = "nested:";

  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    return new NestedUrlConnection(url);
  }

  /**
   * Assert that the specified URL is a valid "nested" URL.
   *
   * @param url the URL to check
   */
  public static void assertUrlIsNotMalformed(String url) {
    if (url == null || !url.startsWith(PREFIX)) {
      throw new IllegalArgumentException("'url' must not be null and must use 'nested' protocol");
    }
    NestedLocation.parse(url.substring(PREFIX.length()));
  }

  /**
   * Clear any internal caches.
   */
  public static void clearCache() {
    NestedLocation.clearCache();
  }

}
