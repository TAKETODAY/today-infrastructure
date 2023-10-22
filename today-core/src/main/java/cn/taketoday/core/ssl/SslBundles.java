/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.function.Consumer;

/**
 * A managed set of {@link SslBundle} instances that can be retrieved by name.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface SslBundles {

  /**
   * Return an {@link SslBundle} with the provided name.
   *
   * @param name the bundle name
   * @return the bundle
   * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
   */
  SslBundle getBundle(String name) throws NoSuchSslBundleException;

  /**
   * Add a handler that will be called each time the named bundle is updated.
   *
   * @param name the bundle name
   * @param updateHandler the handler that should be called
   * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
   */
  void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) throws NoSuchSslBundleException;

}
