/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.config;

import java.util.Map;

import cn.taketoday.web.view.UrlBasedViewResolver;

/**
 * Assist with configuring a {@link UrlBasedViewResolver}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 15:43
 */
public class UrlBasedViewResolverRegistration {

  protected final UrlBasedViewResolver viewResolver;

  public UrlBasedViewResolverRegistration(UrlBasedViewResolver viewResolver) {
    this.viewResolver = viewResolver;
  }

  protected UrlBasedViewResolver getViewResolver() {
    return this.viewResolver;
  }

  /**
   * Set the prefix that gets prepended to view names when building a URL.
   *
   * @see UrlBasedViewResolver#setPrefix
   */
  public UrlBasedViewResolverRegistration prefix(String prefix) {
    this.viewResolver.setPrefix(prefix);
    return this;
  }

  /**
   * Set the suffix that gets appended to view names when building a URL.
   *
   * @see cn.taketoday.web.servlet.view.UrlBasedViewResolver#setSuffix
   */
  public UrlBasedViewResolverRegistration suffix(String suffix) {
    this.viewResolver.setSuffix(suffix);
    return this;
  }

  /**
   * Set the view class that should be used to create views.
   *
   * @see cn.taketoday.web.servlet.view.UrlBasedViewResolver#setViewClass
   */
  public UrlBasedViewResolverRegistration viewClass(Class<?> viewClass) {
    this.viewResolver.setViewClass(viewClass);
    return this;
  }

  /**
   * Set the view names (or name patterns) that can be handled by this view
   * resolver. View names can contain simple wildcards such that 'my*', '*Report'
   * and '*Repo*' will all match the view name 'myReport'.
   *
   * @see cn.taketoday.web.servlet.view.UrlBasedViewResolver#setViewNames
   */
  public UrlBasedViewResolverRegistration viewNames(String... viewNames) {
    this.viewResolver.setViewNames(viewNames);
    return this;
  }

  /**
   * Set static attributes to be added to the model of every request for all
   * views resolved by this view resolver. This allows for setting any kind of
   * attribute values, for example bean references.
   *
   * @see cn.taketoday.web.servlet.view.UrlBasedViewResolver#setAttributesMap
   */
  public UrlBasedViewResolverRegistration attributes(Map<String, ?> attributes) {
    this.viewResolver.setAttributesMap(attributes);
    return this;
  }

  /**
   * Specify the maximum number of entries for the view cache.
   * Default is 1024.
   *
   * @see cn.taketoday.web.servlet.view.UrlBasedViewResolver#setCache(boolean)
   */
  public UrlBasedViewResolverRegistration cacheLimit(int cacheLimit) {
    this.viewResolver.setCacheLimit(cacheLimit);
    return this;
  }

  /**
   * Enable or disable caching.
   * <p>This is equivalent to setting the {@link #cacheLimit "cacheLimit"}
   * property to the default limit (1024) or to 0, respectively.
   * <p>Default is "true": caching is enabled.
   * Disable this only for debugging and development.
   *
   * @see cn.taketoday.web.servlet.view.UrlBasedViewResolver#setCache(boolean)
   */
  public UrlBasedViewResolverRegistration cache(boolean cache) {
    this.viewResolver.setCache(cache);
    return this;
  }

}


