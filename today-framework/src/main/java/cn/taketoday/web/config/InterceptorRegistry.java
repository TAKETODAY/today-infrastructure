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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.Ordered;
import cn.taketoday.web.HandlerInterceptor;

/**
 * @author TODAY 2021/8/30 21:43
 */
public class InterceptorRegistry {

  private final List<InterceptorRegistration> registrations = new ArrayList<>();

  /**
   * Adds the provided {@link HandlerInterceptor}.
   *
   * @param interceptor the interceptor to add
   * @return an {@link InterceptorRegistration} that allows you optionally configure the
   * registered interceptor further for example adding URL patterns it should apply to.
   */
  public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
    InterceptorRegistration registration = new InterceptorRegistration(interceptor);
    this.registrations.add(registration);
    return registration;
  }

  /**
   * Return all registered interceptors.
   */
  protected List<Object> getInterceptors() {
    return this.registrations.stream()
            .sorted(INTERCEPTOR_ORDER_COMPARATOR)
            .map(InterceptorRegistration::getInterceptor)
            .collect(Collectors.toList());
  }

  private static final Comparator<Object> INTERCEPTOR_ORDER_COMPARATOR =
          OrderComparator.INSTANCE.withSourceProvider(object -> {
            if (object instanceof InterceptorRegistration) {
              return (Ordered) ((InterceptorRegistration) object)::getOrder;
            }
            return null;
          });

}
