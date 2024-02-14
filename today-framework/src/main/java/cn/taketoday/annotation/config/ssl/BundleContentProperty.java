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

package cn.taketoday.annotation.config.ssl;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;

import cn.taketoday.core.ssl.pem.PemContent;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

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

  Path toWatchPath() {
    return toPath();
  }

  private Path toPath() {
    try {
      URL url = toUrl();
      Assert.state(isFileUrl(url), () -> "Value '%s' is not a file URL".formatted(url));
      return Path.of(url.toURI()).toAbsolutePath();
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to convert value of property '%s' to a path".formatted(this.name),
              ex);
    }
  }

  private URL toUrl() throws FileNotFoundException {
    Assert.state(!isPemContent(), "Value contains PEM content");
    return ResourceUtils.getURL(this.value);
  }

  private boolean isFileUrl(URL url) {
    return "file".equalsIgnoreCase(url.getProtocol());
  }

}
