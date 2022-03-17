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
package cn.taketoday.web.registry;

import java.util.Collection;

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;

import static java.util.Objects.requireNonNull;

/**
 * @author TODAY 2020/12/10 23:31
 */
public class CompositeHandlerCustomizer implements HandlerCustomizer {
  private final HandlerCustomizer[] customizers;

  public CompositeHandlerCustomizer(HandlerCustomizer... customizers) {
    AnnotationAwareOrderComparator.sort(customizers);
    this.customizers = customizers;
  }

  public CompositeHandlerCustomizer(final Collection<HandlerCustomizer> customizers) {
    this(requireNonNull(customizers).toArray(new HandlerCustomizer[customizers.size()]));
  }

  @Override
  public Object customize(String handlerKey, Object handler) {
    for (final HandlerCustomizer customizer : customizers) {
      handler = customizer.customize(handlerKey, handler);
    }
    return handler;
  }
}
