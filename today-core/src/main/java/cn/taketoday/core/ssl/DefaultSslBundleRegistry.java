/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.ssl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Assert;

/**
 * Default {@link SslBundleRegistry} implementation.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

  private final Map<String, SslBundle> bundles = new ConcurrentHashMap<>();

  public DefaultSslBundleRegistry() { }

  public DefaultSslBundleRegistry(String name, SslBundle bundle) {
    registerBundle(name, bundle);
  }

  @Override
  public void registerBundle(String name, SslBundle bundle) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(bundle, "Bundle must not be null");
    SslBundle previous = this.bundles.putIfAbsent(name, bundle);
    if (previous != null) {
      throw new IllegalStateException("Cannot replace existing SSL bundle '%s'".formatted(name));
    }
  }

  @Override
  public SslBundle getBundle(String name) {
    Assert.notNull(name, "Name must not be null");
    SslBundle bundle = this.bundles.get(name);
    if (bundle == null) {
      throw new NoSuchSslBundleException(name, "SSL bundle name '%s' cannot be found".formatted(name));
    }
    return bundle;
  }

}
