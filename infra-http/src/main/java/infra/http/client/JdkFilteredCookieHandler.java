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

package infra.http.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JDK {@link CookieHandler} to check a URL is not filtered by a
 * {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class JdkFilteredCookieHandler extends CookieHandler {

  private final @Nullable CookieHandler delegate;

  private final UrlHostFilter urlHostFilter;

  JdkFilteredCookieHandler(@Nullable CookieHandler delegate, UrlHostFilter urlHostFilter) {
    this.delegate = delegate;
    this.urlHostFilter = urlHostFilter;
  }

  @Override
  public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
    this.urlHostFilter.throwIfFiltered(uri);
    return (this.delegate != null) ? this.delegate.get(uri, requestHeaders) : Collections.emptyMap();

  }

  @Override
  public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
    if (this.delegate != null) {
      this.delegate.put(uri, responseHeaders);
    }
  }

}
