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

package cn.taketoday.app.loader.jar;

import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import cn.taketoday.app.loader.zip.ZipContent;

/**
 * Info obtained from a {@link ZipContent} instance relating to the {@link Manifest}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ManifestInfo {

  private static final Name MULTI_RELEASE = new Name("Multi-Release");

  static final ManifestInfo NONE = new ManifestInfo(null, false);

  private final Manifest manifest;

  private volatile Boolean multiRelease;

  /**
   * Create a new {@link ManifestInfo} instance.
   *
   * @param manifest the jar manifest
   */
  ManifestInfo(Manifest manifest) {
    this(manifest, null);
  }

  private ManifestInfo(Manifest manifest, Boolean multiRelease) {
    this.manifest = manifest;
    this.multiRelease = multiRelease;
  }

  /**
   * Return the manifest, if any.
   *
   * @return the manifest or {@code null}
   */
  Manifest getManifest() {
    return this.manifest;
  }

  /**
   * Return if this is a multi-release jar.
   *
   * @return if the jar is multi-release
   */
  boolean isMultiRelease() {
    if (this.manifest == null) {
      return false;
    }
    Boolean multiRelease = this.multiRelease;
    if (multiRelease != null) {
      return multiRelease;
    }
    Attributes attributes = this.manifest.getMainAttributes();
    multiRelease = attributes.containsKey(MULTI_RELEASE);
    this.multiRelease = multiRelease;
    return multiRelease;
  }

}
