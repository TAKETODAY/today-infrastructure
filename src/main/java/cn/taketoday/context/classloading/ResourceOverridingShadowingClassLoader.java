/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.classloading;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Subclass of ShadowingClassLoader that overrides attempts to
 * locate certain files.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 4.0
 */
public class ResourceOverridingShadowingClassLoader extends ShadowingClassLoader {

  private static final Enumeration<URL> EMPTY_URL_ENUMERATION = new Enumeration<>() {
    @Override
    public boolean hasMoreElements() {
      return false;
    }

    @Override
    public URL nextElement() {
      throw new UnsupportedOperationException("Should not be called. I am empty.");
    }
  };

  /**
   * Key is asked for value: value is actual value.
   */
  private final Map<String, String> overrides = new HashMap<>();

  /**
   * Create a new ResourceOverridingShadowingClassLoader,
   * decorating the given ClassLoader.
   *
   * @param enclosingClassLoader the ClassLoader to decorate
   */
  public ResourceOverridingShadowingClassLoader(ClassLoader enclosingClassLoader) {
    super(enclosingClassLoader);
  }

  /**
   * Return the resource (if any) at the new path
   * on an attempt to locate a resource at the old path.
   *
   * @param oldPath the path requested
   * @param newPath the actual path to be looked up
   */
  public void override(String oldPath, String newPath) {
    this.overrides.put(oldPath, newPath);
  }

  /**
   * Ensure that a resource with the given path is not found.
   *
   * @param oldPath the path of the resource to hide even if
   * it exists in the parent ClassLoader
   */
  public void suppress(String oldPath) {
    this.overrides.put(oldPath, null);
  }

  /**
   * Copy all overrides from the given ClassLoader.
   *
   * @param other the other ClassLoader to copy from
   */
  public void copyOverrides(ResourceOverridingShadowingClassLoader other) {
    Assert.notNull(other, "Other ClassLoader must not be null");
    this.overrides.putAll(other.overrides);
  }

  @Override
  public URL getResource(String requestedPath) {
    if (this.overrides.containsKey(requestedPath)) {
      String overriddenPath = this.overrides.get(requestedPath);
      return (overriddenPath != null ? super.getResource(overriddenPath) : null);
    }
    else {
      return super.getResource(requestedPath);
    }
  }

  @Override
  @Nullable
  public InputStream getResourceAsStream(String requestedPath) {
    if (this.overrides.containsKey(requestedPath)) {
      String overriddenPath = this.overrides.get(requestedPath);
      return (overriddenPath != null ? super.getResourceAsStream(overriddenPath) : null);
    }
    else {
      return super.getResourceAsStream(requestedPath);
    }
  }

  @Override
  public Enumeration<URL> getResources(String requestedPath) throws IOException {
    if (this.overrides.containsKey(requestedPath)) {
      String overriddenLocation = this.overrides.get(requestedPath);
      return (overriddenLocation != null ?
              super.getResources(overriddenLocation) : EMPTY_URL_ENUMERATION);
    }
    else {
      return super.getResources(requestedPath);
    }
  }

}
