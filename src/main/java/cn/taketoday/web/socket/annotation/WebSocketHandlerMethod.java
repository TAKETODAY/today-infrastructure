/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.handler.MethodParametersBuilder;

import java.lang.reflect.Method;

/**
 * @author TODAY 2021/5/7 15:32
 * @since 3.0.1
 */
public class WebSocketHandlerMethod {
  private final Object bean; // controller bean
  /** action **/
  private final Method method;

  private final MethodInvoker handlerInvoker;

  private final MethodParameter[] parameters;

  public WebSocketHandlerMethod(Object bean, Method method, MethodParametersBuilder parameterBuilder) {
    this.bean = bean;
    this.method = method;
    this.handlerInvoker = MethodInvoker.fromMethod(method);
    this.parameters = parameterBuilder.build(method);
  }

  public void invoke(Object[] args) {
    handlerInvoker.invoke(bean, args);
  }

  public MethodParameter[] getParameters() {
    return parameters;
  }

  public Method getMethod() {
    return method;
  }
}
