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

import java.io.IOException;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * Provides access {@link Manifest} content that can be safely returned from
 * {@link UrlJarFile} or {@link UrlNestedJarFile}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class UrlJarManifest {

  private static final Object NONE = new Object();

  private final ManifestSupplier supplier;

  private volatile Object supplied;

  UrlJarManifest(ManifestSupplier supplier) {
    this.supplier = supplier;
  }

  Manifest get() throws IOException {
    Manifest manifest = supply();
    if (manifest == null) {
      return null;
    }
    Manifest copy = new Manifest();
    copy.getMainAttributes().putAll((Map<?, ?>) manifest.getMainAttributes().clone());
    manifest.getEntries().forEach((key, value) -> copy.getEntries().put(key, cloneAttributes(value)));
    return copy;
  }

  Attributes getEntryAttributes(JarEntry entry) throws IOException {
    Manifest manifest = supply();
    if (manifest == null) {
      return null;
    }
    Attributes attributes = manifest.getEntries().get(entry.getName());
    return cloneAttributes(attributes);
  }

  private Attributes cloneAttributes(Attributes attributes) {
    return (attributes != null) ? (Attributes) attributes.clone() : null;
  }

  private Manifest supply() throws IOException {
    Object supplied = this.supplied;
    if (supplied == null) {
      supplied = this.supplier.getManifest();
      this.supplied = (supplied != null) ? supplied : NONE;
    }
    return (supplied != NONE) ? (Manifest) supplied : null;
  }

  /**
   * Interface used to supply the actual manifest.
   */
  @FunctionalInterface
  interface ManifestSupplier {

    Manifest getManifest() throws IOException;

  }

}
