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

package cn.taketoday.web.handler.method;

import cn.taketoday.web.handler.HandlerMethodMappingNamingStrategy;

/**
 * A {@link HandlerMethodMappingNamingStrategy HandlerMethodMappingNamingStrategy}
 * for {@code RequestMappingInfo}-based handler method mappings.
 *
 * If the {@code RequestMappingInfo} name attribute is set, its value is used.
 * Otherwise the name is based on the capital letters of the class name,
 * followed by "#" as a separator, and the method name. For example "TC#getFoo"
 * for a class named TestController with method getFoo.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RequestMappingInfoHandlerMethodMappingNamingStrategy
        implements HandlerMethodMappingNamingStrategy<RequestMappingInfo> {

  /** Separator between the type and method-level parts of a HandlerMethod mapping name. */
  public static final String SEPARATOR = "#";

  @Override
  public String getName(HandlerMethod handlerMethod, RequestMappingInfo mapping) {
    if (mapping.getName() != null) {
      return mapping.getName();
    }
    StringBuilder sb = new StringBuilder();
    String simpleTypeName = handlerMethod.getBeanType().getSimpleName();
    for (int i = 0; i < simpleTypeName.length(); i++) {
      if (Character.isUpperCase(simpleTypeName.charAt(i))) {
        sb.append(simpleTypeName.charAt(i));
      }
    }
    sb.append(SEPARATOR)
            .append(handlerMethod.getMethod().getName());
    return sb.toString();
  }

}
