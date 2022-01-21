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

package cn.taketoday.http.client.support;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.InterceptingClientHttpRequestFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Base class for {@link cn.taketoday.web.client.RestTemplate}
 * and other HTTP accessing gateway helpers, adding interceptor-related
 * properties to {@link HttpAccessor}'s common properties.
 *
 * <p>Not intended to be used directly.
 * See {@link cn.taketoday.web.client.RestTemplate} for an entry point.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see ClientHttpRequestInterceptor
 * @see InterceptingClientHttpRequestFactory
 * @see cn.taketoday.web.client.RestTemplate
 * @since 4.0
 */
public abstract class InterceptingHttpAccessor extends HttpAccessor {

  private final ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

  @Nullable
  private volatile ClientHttpRequestFactory interceptingRequestFactory;

  /**
   * Set the request interceptors that this accessor should use.
   * <p>The interceptors will get immediately sorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order}.
   *
   * @see #getRequestFactory()
   * @see AnnotationAwareOrderComparator
   */
  public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
    Assert.noNullElements(interceptors, "'interceptors' must not contain null elements");
    // Take getInterceptors() List as-is when passed in here
    if (this.interceptors != interceptors) {
      this.interceptors.clear();
      this.interceptors.addAll(interceptors);
      this.interceptors.trimToSize();
      AnnotationAwareOrderComparator.sort(this.interceptors);
    }
  }

  /**
   * Set the request interceptors that this accessor should use.
   * <p>The interceptors will get immediately sorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order}.
   *
   * @see #getRequestFactory()
   * @see AnnotationAwareOrderComparator
   */
  public void setInterceptors(ClientHttpRequestInterceptor... interceptors) {
    Assert.noNullElements(interceptors, "'interceptors' must not contain null elements");
    this.interceptors.clear();
    CollectionUtils.addAll(this.interceptors, interceptors);
    this.interceptors.trimToSize();
    AnnotationAwareOrderComparator.sort(this.interceptors);
  }

  /**
   * Get the request interceptors that this accessor uses.
   * <p>The returned {@link List} is active and may be modified. Note,
   * however, that the interceptors will not be resorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order} before the
   * {@link ClientHttpRequestFactory} is built.
   */
  public List<ClientHttpRequestInterceptor> getInterceptors() {
    return this.interceptors;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
    super.setRequestFactory(requestFactory);
    this.interceptingRequestFactory = null;
  }

  /**
   * Overridden to expose an {@link InterceptingClientHttpRequestFactory}
   * if necessary.
   *
   * @see #getInterceptors()
   */
  @Override
  public ClientHttpRequestFactory getRequestFactory() {
    if (!interceptors.isEmpty()) {
      ClientHttpRequestFactory factory = this.interceptingRequestFactory;
      if (factory == null) {
        synchronized(this) {
          factory = this.interceptingRequestFactory;
          if (factory == null) {
            factory = new InterceptingClientHttpRequestFactory(super.getRequestFactory(), interceptors);
            this.interceptingRequestFactory = factory;
          }
        }
      }
      return factory;
    }
    else {
      return super.getRequestFactory();
    }
  }

}
