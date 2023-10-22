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

/**
 * Interface that can be used to register an {@link SslBundle} for a given name.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface SslBundleRegistry {

  /**
   * Register a named {@link SslBundle}.
   *
   * @param name the bundle name
   * @param bundle the bundle
   */
  void registerBundle(String name, SslBundle bundle);

  /**
   * Updates an {@link SslBundle}.
   *
   * @param name the bundle name
   * @param updatedBundle the updated bundle
   * @throws NoSuchSslBundleException if the bundle cannot be found
   */
  void updateBundle(String name, SslBundle updatedBundle) throws NoSuchSslBundleException;

}
