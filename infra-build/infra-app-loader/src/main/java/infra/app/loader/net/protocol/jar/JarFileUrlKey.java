/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.loader.net.protocol.jar;

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Locale;
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
    if (!isCachableUrl(url)) {
      return create(url);
    }
    Map<URL, String> cache = (JarFileUrlKey.cache != null) ? JarFileUrlKey.cache.get() : null;
    if (cache == null) {
      cache = new ConcurrentHashMap<>();
      JarFileUrlKey.cache = new SoftReference<>(cache);
    }
    return cache.computeIfAbsent(url, JarFileUrlKey::create);
  }

  private static boolean isCachableUrl(URL url) {
    // Don't cache URL that have a host since equals() will perform DNS lookup
    return url.getHost() == null || url.getHost().isEmpty();
  }

  private static String create(URL url) {
    String protocol = url.getProtocol();
    String host = url.getHost();
    int port = (url.getPort() != -1) ? url.getPort() : url.getDefaultPort();
    String file = url.getFile();
    return get(protocol, host, port, file, "runtime".equals(url.getRef()));
  }

  /**
   * Generate a cache key from already-parsed URL components.
   * @param protocol the URL protocol
   * @param host the URL host
   * @param port the effective port, or -1
   * @param file the URL file specification
   * @param runtimeRef whether the runtime fragment is present
   * @return the normalized string cache key
   */
  static String get(String protocol, String host, int port, String file, boolean runtimeRef) {
    StringBuilder value = new StringBuilder();
    value.append(protocol.toLowerCase(Locale.ROOT));
    value.append(":");
    if (host != null && !host.isEmpty()) {
      value.append(host.toLowerCase(Locale.ROOT));
      value.append((port != -1) ? ":" + port : "");
    }
    value.append((file != null) ? file : "");
    if (runtimeRef) {
      value.append("#runtime");
    }
    return value.toString();
  }

  static void clearCache() {
    cache = null;
  }

}
