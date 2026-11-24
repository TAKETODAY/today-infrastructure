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

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

/**
 * JDK {@link ProxySelector} to check a URL is not filtered by a
 * {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class JdkFilteredProxySelector extends ProxySelector {

  private final ProxySelector delegate;

  private final UrlHostFilter urlHostFilter;

  JdkFilteredProxySelector(ProxySelector delegate, UrlHostFilter urlHostFilter) {
    this.delegate = delegate;
    this.urlHostFilter = urlHostFilter;
  }

  @Override
  public List<Proxy> select(URI uri) {
    this.urlHostFilter.throwIfFiltered(uri);
    return this.delegate.select(uri);
  }

  @Override
  public void connectFailed(URI uri, SocketAddress address, IOException ex) {
    this.delegate.connectFailed(uri, address, ex);
  }

}
