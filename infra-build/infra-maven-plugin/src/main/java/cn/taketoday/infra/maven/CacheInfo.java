/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.infra.maven;

import cn.taketoday.buildpack.platform.build.Cache;

import cn.taketoday.lang.Assert;

/**
 * Encapsulates configuration of an image building cache.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CacheInfo {

  private Cache cache;

  public CacheInfo() { }

  CacheInfo(VolumeCacheInfo volumeCacheInfo) {
    this.cache = Cache.volume(volumeCacheInfo.getName());
  }

  public void setVolume(VolumeCacheInfo info) {
    Assert.state(this.cache == null, "Each image building cache can be configured only once");
    this.cache = Cache.volume(info.getName());
  }

  Cache asCache() {
    return this.cache;
  }

  /**
   * Encapsulates configuration of an image building cache stored in a volume.
   */
  public static class VolumeCacheInfo {

    private String name;

    VolumeCacheInfo(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    void setName(String name) {
      this.name = name;
    }

  }

}
