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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import javax.inject.Inject;

import cn.taketoday.buildpack.platform.build.Cache;

/**
 * Configuration for an image building cache.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CacheSpec {

  private final ObjectFactory objectFactory;

  private Cache cache = null;

  @Inject
  public CacheSpec(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public Cache asCache() {
    return this.cache;
  }

  /**
   * Configures a volume cache using the given {@code action}.
   *
   * @param action the action
   */
  public void volume(Action<VolumeCacheSpec> action) {
    if (this.cache != null) {
      throw new GradleException("Each image building cache can be configured only once");
    }
    VolumeCacheSpec spec = this.objectFactory.newInstance(VolumeCacheSpec.class);
    action.execute(spec);
    this.cache = Cache.volume(spec.getName().get());
  }

  /**
   * Configures a bind cache using the given {@code action}.
   *
   * @param action the action
   */
  public void bind(Action<BindCacheSpec> action) {
    if (this.cache != null) {
      throw new GradleException("Each image building cache can be configured only once");
    }
    BindCacheSpec spec = this.objectFactory.newInstance(BindCacheSpec.class);
    action.execute(spec);
    this.cache = Cache.bind(spec.getSource().get());
  }

  /**
   * Configuration for an image building cache stored in a Docker volume.
   */
  public abstract static class VolumeCacheSpec {

    /**
     * Returns the name of the cache.
     *
     * @return the cache name
     */
    @Input
    public abstract Property<String> getName();

  }

  /**
   * Configuration for an image building cache stored in a bind mount.
   */
  public abstract static class BindCacheSpec {

    /**
     * Returns the source of the cache.
     *
     * @return the cache source
     */
    @Input
    public abstract Property<String> getSource();

  }

}
