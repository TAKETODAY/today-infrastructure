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

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * for HandlerMethod return-value
 *
 * @author TODAY 2019-12-13 13:52
 */
public abstract class HandlerMethodReturnValueHandler
        extends OrderedSupport implements ReturnValueHandler {

  @Override
  public final boolean supportsHandler(final Object handler) {
    return handler instanceof ActionMappingAnnotationHandler annotationHandler
            && supportsHandlerMethod(annotationHandler.getMethod());
  }

  /**
   * match function for {@link HandlerMethod}
   *
   * @see HandlerMethod
   */
  protected abstract boolean supportsHandlerMethod(HandlerMethod handler);

}
