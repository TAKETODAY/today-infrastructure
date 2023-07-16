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

package cn.taketoday.buildpack.platform.build;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * An opaque reference to a {@link Buildpack}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BuildpackResolver
 * @since 4.0
 */
public final class BuildpackReference {

  private final String value;

  private BuildpackReference(String value) {
    this.value = value;
  }

  boolean hasPrefix(String prefix) {
    return this.value.startsWith(prefix);
  }

  @Nullable
  String getSubReference(String prefix) {
    return this.value.startsWith(prefix) ? this.value.substring(prefix.length()) : null;
  }

  Path asPath() {
    try {
      URL url = new URL(this.value);
      if (url.getProtocol().equals("file")) {
        return Paths.get(url.getPath());
      }
      return null;
    }
    catch (MalformedURLException ex) {
      // not a URL, fall through to attempting to find a plain file path
    }
    try {
      return Paths.get(this.value);
    }
    catch (Exception ex) {
      return null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.value.equals(((BuildpackReference) obj).value);
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public String toString() {
    return this.value;
  }

  /**
   * Create a new {@link BuildpackReference} from the given value.
   *
   * @param value the value to use
   * @return a new {@link BuildpackReference}
   */
  public static BuildpackReference of(String value) {
    Assert.hasText(value, "Value must not be empty");
    return new BuildpackReference(value);
  }

}
