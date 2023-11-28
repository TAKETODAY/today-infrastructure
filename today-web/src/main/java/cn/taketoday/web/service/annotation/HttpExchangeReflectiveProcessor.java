/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.service.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cn.taketoday.aot.hint.BindingReflectionHintsRegistrar;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.annotation.ReflectiveProcessor;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.web.annotation.RequestBody;

/**
 * {@link ReflectiveProcessor} implementation for {@link HttpExchange @HttpExchange}
 * annotated methods. In addition to registering reflection hints for invoking
 * the annotated method, this implementation handles reflection-based
 * binding for return types and parameters annotated with {@link RequestBody}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class HttpExchangeReflectiveProcessor implements ReflectiveProcessor {

  private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

  @Override
  public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    if (element instanceof Method method) {
      registerMethodHints(hints, method);
    }
  }

  protected void registerMethodHints(ReflectionHints hints, Method method) {
    hints.registerMethod(method, ExecutableMode.INVOKE);
    for (Parameter parameter : method.getParameters()) {
      registerParameterTypeHints(hints, MethodParameter.forParameter(parameter));
    }
    registerReturnTypeHints(hints, MethodParameter.forExecutable(method, -1));
  }

  protected void registerParameterTypeHints(ReflectionHints hints, MethodParameter methodParameter) {
    if (methodParameter.hasParameterAnnotation(RequestBody.class)) {
      this.bindingRegistrar.registerReflectionHints(hints, methodParameter.getGenericParameterType());
    }
  }

  protected void registerReturnTypeHints(ReflectionHints hints, MethodParameter returnTypeParameter) {
    if (!void.class.equals(returnTypeParameter.getParameterType())) {
      this.bindingRegistrar.registerReflectionHints(hints, returnTypeParameter.getGenericParameterType());
    }
  }

}
