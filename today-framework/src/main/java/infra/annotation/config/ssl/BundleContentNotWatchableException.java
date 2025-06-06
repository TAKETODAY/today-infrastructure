/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.ssl;

/**
 * Thrown when a bundle content location is not watchable.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class BundleContentNotWatchableException extends RuntimeException {

  private final BundleContentProperty property;

  BundleContentNotWatchableException(BundleContentProperty property) {
    super("The content of '%s' is not watchable. Only 'file:' resources are watchable, but '%s' has been set"
            .formatted(property.name(), property.value()));
    this.property = property;
  }

  private BundleContentNotWatchableException(String bundleName, BundleContentProperty property, Throwable cause) {
    super("The content of '%s' from bundle '%s' is not watchable'. Only 'file:' resources are watchable, but '%s' has been set"
            .formatted(property.name(), bundleName, property.value()), cause);
    this.property = property;
  }

  BundleContentNotWatchableException withBundleName(String bundleName) {
    return new BundleContentNotWatchableException(bundleName, this.property, this);
  }

}
