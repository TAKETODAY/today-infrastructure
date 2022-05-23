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

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ArrayHolder;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerInterceptorsProvider;

/**
 * for holding {@link HandlerInterceptor}s
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/18 10:26
 */
public class HandlerInterceptorHolder implements HandlerInterceptorsProvider {

  /** interceptors array */
  protected final ArrayHolder<HandlerInterceptor> interceptors = ArrayHolder.forGenerator(HandlerInterceptor[]::new);

  /**
   * replace interceptors
   *
   * @param interceptors interceptors to add
   */
  public void setInterceptors(HandlerInterceptor... interceptors) {
    this.interceptors.set(interceptors);
  }

  /**
   * add interceptors at end of the {@link #interceptors}
   *
   * @param interceptors interceptors to add
   * @throws NullPointerException interceptors is null
   */
  public void addInterceptors(HandlerInterceptor... interceptors) {
    this.interceptors.add(interceptors);
  }

  /**
   * add interceptors at end of the {@link #interceptors}
   *
   * @param interceptors interceptors to add
   * @throws NullPointerException interceptors is null
   */
  public void addInterceptors(List<HandlerInterceptor> interceptors) {
    this.interceptors.addAll(interceptors);
  }

  public void setInterceptors(@Nullable List<HandlerInterceptor> interceptors) {
    this.interceptors.set(interceptors);
  }

  @Override
  @Nullable
  public HandlerInterceptor[] getInterceptors() {
    return interceptors.get();
  }

  @Override
  public boolean hasInterceptor() {
    return interceptors.isPresent();
  }

  public ArrayHolder<HandlerInterceptor> getHolder() {
    return interceptors;
  }

}
