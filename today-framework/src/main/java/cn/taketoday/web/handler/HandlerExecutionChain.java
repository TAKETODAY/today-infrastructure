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

package cn.taketoday.web.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.registry.HandlerMapping;

/**
 * Handler execution chain, consisting of handler object and any handler interceptors.
 * Returned by HandlerMapping's {@link HandlerMapping#getHandler} method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerInterceptor
 * @since 4.0 2022/5/22 22:42
 */
public class HandlerExecutionChain {

  private final Object handler;

  private final ArrayList<HandlerInterceptor> interceptorList = new ArrayList<>();

  /**
   * Create a new HandlerExecutionChain.
   *
   * @param handler the handler object to execute
   */
  public HandlerExecutionChain(Object handler) {
    this(handler, (HandlerInterceptor[]) null);
  }

  /**
   * Create a new HandlerExecutionChain.
   *
   * @param handler the handler object to execute
   * @param interceptors the array of interceptors to apply
   * (in the given order) before the handler itself executes
   */
  public HandlerExecutionChain(Object handler, @Nullable HandlerInterceptor... interceptors) {
    this(handler, (interceptors != null ? Arrays.asList(interceptors) : Collections.emptyList()));
  }

  /**
   * Create a new HandlerExecutionChain.
   *
   * @param handler the handler object to execute
   * @param interceptorList the list of interceptors to apply
   * (in the given order) before the handler itself executes
   */
  public HandlerExecutionChain(Object handler, List<HandlerInterceptor> interceptorList) {
    if (handler instanceof HandlerExecutionChain originalChain) {
      this.handler = originalChain.getHandler();
      this.interceptorList.addAll(originalChain.interceptorList);
    }
    else {
      this.handler = handler;
    }
    this.interceptorList.addAll(interceptorList);
  }

  /**
   * Return the handler object to execute.
   */
  public Object getHandler() {
    return this.handler;
  }

  /**
   * Add the given interceptor to the end of this chain.
   */
  public void addInterceptor(HandlerInterceptor interceptor) {
    this.interceptorList.add(interceptor);
  }

  /**
   * Add the given interceptor at the specified index of this chain.
   */
  public void addInterceptor(int index, HandlerInterceptor interceptor) {
    this.interceptorList.add(index, interceptor);
  }

  /**
   * Add the given interceptors to the end of this chain.
   */
  public void addInterceptors(HandlerInterceptor... interceptors) {
    CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
  }

  /**
   * Return the array of interceptors to apply (in the given order).
   *
   * @return the array of HandlerInterceptors instances (may be {@code null})
   */
  @Nullable
  public HandlerInterceptor[] getInterceptors() {
    return (!this.interceptorList.isEmpty() ? this.interceptorList.toArray(HandlerInterceptor.EMPTY_ARRAY) : null);
  }

  /**
   * Return the list of interceptors to apply (in the given order).
   *
   * @return the list of HandlerInterceptors instances (potentially empty)
   */
  public List<HandlerInterceptor> getInterceptorList() {
    return (!this.interceptorList.isEmpty() ? Collections.unmodifiableList(this.interceptorList) :
            Collections.emptyList());
  }

  /**
   * Delegates to the handler's {@code toString()} implementation.
   */
  @Override
  public String toString() {
    return "HandlerExecutionChain with [" + getHandler() + "] and " + this.interceptorList.size() + " interceptors";
  }

}
