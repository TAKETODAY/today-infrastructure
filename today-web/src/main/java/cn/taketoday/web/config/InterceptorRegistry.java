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

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.OrderComparator;
import cn.taketoday.web.HandlerInterceptor;

/**
 * Helps with configuring a list of mapped interceptors.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/8/30 21:43
 */
public class InterceptorRegistry {

  private final ArrayList<InterceptorRegistration> registrations = new ArrayList<>();

  /**
   * Adds the provided {@link HandlerInterceptor}.
   *
   * @param interceptor the interceptor to add
   * @return an {@link InterceptorRegistration} that allows you optionally configure the
   * registered interceptor further for example adding URL patterns it should apply to.
   */
  public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
    InterceptorRegistration registration = new InterceptorRegistration(interceptor);
    registrations.add(registration);
    return registration;
  }

  /**
   * Return all registered interceptors.
   */
  protected List<Object> getInterceptors() {
    OrderComparator.sort(registrations);
    ArrayList<Object> interceptors = new ArrayList<>(registrations.size());
    for (InterceptorRegistration registration : registrations) {
      interceptors.add(registration.getInterceptor());
    }
    return interceptors;
  }

}
