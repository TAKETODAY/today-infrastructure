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

import java.nio.file.Path;

import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.core.ssl.pem.PemContent;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Helper utility to manage a single bundle content configuration property. May possibly
 * contain PEM content, a location or a directory search pattern.
 *
 * @param name the configuration property name (excluding any prefix)
 * @param value the configuration property value
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
record BundleContentProperty(String name, String value) {

  /**
   * Return if the property value is PEM content.
   *
   * @return if the value is PEM content
   */
  boolean isPemContent() {
    return PemContent.isPresentInText(this.value);
  }

  /**
   * Return if there is any property value present.
   *
   * @return if the value is present
   */
  boolean hasValue() {
    return StringUtils.hasText(this.value);
  }

  Path toWatchPath(ResourceLoader resourceLoader) {
    try {
      Assert.state(!isPemContent(), "Value contains PEM content");
      Resource resource = resourceLoader.getResource(this.value);
      if (!resource.isFile()) {
        throw new BundleContentNotWatchableException(this);
      }
      return Path.of(resource.getFile().getAbsolutePath());
    }
    catch (Exception ex) {
      if (ex instanceof BundleContentNotWatchableException bundleContentNotWatchableException) {
        throw bundleContentNotWatchableException;
      }
      throw new IllegalStateException("Unable to convert value of property '%s' to a path".formatted(this.name), ex);
    }
  }
}
