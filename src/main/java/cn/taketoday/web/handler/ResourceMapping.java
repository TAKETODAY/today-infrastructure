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
package cn.taketoday.web.handler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.web.http.CacheControl;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptorsCapable;
import lombok.Getter;

/**
 * @author TODAY <br>
 * 2019-05-15 21:43
 * @since 2.3.7
 */
@Getter
public class ResourceMapping implements Serializable, Ordered, HandlerInterceptorsCapable {
  private static final long serialVersionUID = 1L;

  private static final int DEFAULT_BUFFER_SIZE = 8192;

  private HandlerInterceptor[] interceptors;

  private String[] pathPatterns;

  private CacheControl cacheControl;

  private int bufferSize = DEFAULT_BUFFER_SIZE;

  private long expires = -1;

  private int order;

  private final ArrayList<String> locations = new ArrayList<>();

  public ResourceMapping(HandlerInterceptor[] interceptors, String... pathPatterns) {
    setInterceptors(interceptors);
    setPathPatterns(pathPatterns);
  }

  public ResourceMapping addLocations(String... locations) {
    this.locations.addAll(Arrays.asList(locations));
    return this;
  }

  /**
   * Return the URL path patterns for the resource handler.
   */
  public String[] getPathPatterns() {
    return this.pathPatterns;
  }

  @Override
  public final HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  public CacheControl getCacheControl() {
    return cacheControl;
  }

  public ResourceMapping setInterceptors(HandlerInterceptor... interceptors) {
    this.interceptors = interceptors;
    return this;
  }

  @Override
  public final boolean hasInterceptor() {
    return interceptors != null;
  }

  /**
   * Sets the size of used buffers.
   *
   * @param bufferSize size of buffer
   * @return {@code this}
   */
  public ResourceMapping bufferSize(int bufferSize) {
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("buffer size must be greater than zero");
    }
    this.bufferSize = bufferSize;
    return this;
  }

  /**
   * Sets the default expiration date for the resources.
   *
   * @param count count
   * @param unit time unit
   * @return {@code this}
   */
  public ResourceMapping expires(long count, TimeUnit unit) {
    Assert.notNull(unit, "time unit is required");
    if (count <= 0) {
      throw new IllegalArgumentException("count must be greater than zero");
    }
    this.expires = unit.toMillis(count);
    return this;
  }

  /**
   * Applies the given cache control as header to the response. If the
   * CacheControl is empty, no Cache-Control header is applied to the response.
   *
   * @param cacheControl cache control
   * @return {@code this}
   */
  public ResourceMapping cacheControl(CacheControl cacheControl) {
    Assert.notNull(cacheControl, "cache control is required");
    this.cacheControl = cacheControl;
    return this;
  }

  /**
   * Add pathPatterns to this mapping
   *
   * @param pathPatterns Path patterns
   * @return {@link ResourceMapping}
   * @see ResourceMapping#setPathPatterns(String...)
   */
  public ResourceMapping addPathPatterns(String... pathPatterns) {
    final List<String> pathPatternsList = new ArrayList<>();

    Collections.addAll(pathPatternsList, Objects.requireNonNull(pathPatterns));
    Collections.addAll(pathPatternsList, this.pathPatterns);

    this.pathPatterns = pathPatternsList.toArray(new String[pathPatternsList.size()]);
    return this;
  }

  /**
   * Set pathPatterns to this mapping
   *
   * @param pathPatterns Path patterns
   * @return {@link ResourceMapping}
   * @see ResourceMapping#addPathPatterns(String...)
   */
  public ResourceMapping setPathPatterns(String... pathPatterns) {
    this.pathPatterns = pathPatterns == null
                        ? this.pathPatterns = Constant.EMPTY_STRING_ARRAY
                        : pathPatterns;
    return this;
  }

  @Override
  public int getOrder() {
    return order;
  }

  public ResourceMapping setOrder(int order) {
    this.order = order;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ResourceMapping [locations=")
            .append(locations)
            .append(", interceptors=").append(Arrays.toString(interceptors))
            .append("]");
    return builder.toString();
  }

}
