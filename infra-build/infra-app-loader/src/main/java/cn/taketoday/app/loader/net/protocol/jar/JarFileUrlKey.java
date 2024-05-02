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

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to generate a string key from a jar file {@link URL} that can be used as a
 * cache key.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class JarFileUrlKey {

  private static volatile SoftReference<Map<URL, String>> cache;

  private JarFileUrlKey() {
  }

  /**
   * Get the {@link JarFileUrlKey} for the given URL.
   *
   * @param url the source URL
   * @return a {@link JarFileUrlKey} instance
   */
  static String get(URL url) {
    Map<URL, String> cache = (JarFileUrlKey.cache != null) ? JarFileUrlKey.cache.get() : null;
    if (cache == null) {
      cache = new ConcurrentHashMap<>();
      JarFileUrlKey.cache = new SoftReference<>(cache);
    }
    return cache.computeIfAbsent(url, JarFileUrlKey::create);
  }

  private static String create(URL url) {
    StringBuilder value = new StringBuilder();
    String protocol = url.getProtocol();
    String host = url.getHost();
    int port = (url.getPort() != -1) ? url.getPort() : url.getDefaultPort();
    String file = url.getFile();
    value.append(protocol.toLowerCase());
    value.append(":");
    if (host != null && !host.isEmpty()) {
      value.append(host.toLowerCase());
      value.append((port != -1) ? ":" + port : "");
    }
    value.append((file != null) ? file : "");
    if ("runtime".equals(url.getRef())) {
      value.append("#runtime");
    }
    return value.toString();
  }

  static void clearCache() {
    cache = null;
  }

}
