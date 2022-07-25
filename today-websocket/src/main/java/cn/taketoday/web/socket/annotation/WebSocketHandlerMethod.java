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

package cn.taketoday.web.socket.annotation;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.MethodInvoker;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.handler.method.ResolvableParameterFactory;

/**
 * @author TODAY 2021/5/7 15:32
 * @since 3.0.1
 */
public class WebSocketHandlerMethod {
  private final BeanSupplier<Object> supplier; // controller bean
  /** action **/
  private final Method method;

  private final MethodInvoker handlerInvoker;

  @Nullable
  private final ResolvableMethodParameter[] parameters;

  public WebSocketHandlerMethod(BeanSupplier<Object> supplier, Method method, ResolvableParameterFactory parameterBuilder) {
    this.supplier = supplier;
    this.method = method;
    this.handlerInvoker = MethodInvoker.fromMethod(method);
    this.parameters = parameterBuilder.createArray(method);
  }

  public void invoke(Object[] args) {
    handlerInvoker.invoke(supplier.get(), args);
  }

  @Nullable
  public ResolvableMethodParameter[] getParameters() {
    return parameters;
  }

  public Method getMethod() {
    return method;
  }
}
