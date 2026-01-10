/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import infra.aot.hint.BindingReflectionHintsRegistrar;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.annotation.ReflectiveProcessor;
import infra.core.MethodParameter;
import infra.core.annotation.AnnotatedElementUtils;
import infra.http.HttpEntity;
import infra.stereotype.Controller;
import infra.web.bind.annotation.ModelAttribute;

/**
 * {@link ReflectiveProcessor} implementation for {@link Controller} and
 * controller-specific annotated methods. In addition to registering reflection
 * hints for invoking the annotated method, this implementation handles:
 *
 * <ul>
 *     <li>Return types annotated with {@link ResponseBody}</li>
 *     <li>Parameters annotated with {@link RequestBody}, {@link infra.web.bind.annotation.ModelAttribute} and {@link RequestPart}</li>
 *     <li>{@link HttpEntity} return types and parameters</li>
 * </ul>
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ControllerMappingReflectiveProcessor implements ReflectiveProcessor {

  private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

  @Override
  public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    if (element instanceof Class<?> type) {
      registerTypeHints(hints, type);
    }
    else if (element instanceof Method method) {
      registerMethodHints(hints, method);
    }
  }

  protected final BindingReflectionHintsRegistrar getBindingRegistrar() {
    return this.bindingRegistrar;
  }

  protected void registerTypeHints(ReflectionHints hints, Class<?> type) {
    hints.registerType(type);
  }

  protected void registerMethodHints(ReflectionHints hints, Method method) {
    hints.registerMethod(method, ExecutableMode.INVOKE);
    for (Parameter parameter : method.getParameters()) {
      registerParameterTypeHints(hints, MethodParameter.forParameter(parameter));
    }
    registerReturnTypeHints(hints, MethodParameter.forExecutable(method, -1));
  }

  protected void registerParameterTypeHints(ReflectionHints hints, MethodParameter methodParameter) {
    if (methodParameter.hasParameterAnnotation(RequestBody.class) ||
            methodParameter.hasParameterAnnotation(ModelAttribute.class) ||
            methodParameter.hasParameterAnnotation(RequestPart.class)) {
      this.bindingRegistrar.registerReflectionHints(hints, methodParameter.getGenericParameterType());
    }
    else if (HttpEntity.class.isAssignableFrom(methodParameter.getParameterType())) {
      Type httpEntityType = getHttpEntityType(methodParameter);
      if (httpEntityType != null) {
        this.bindingRegistrar.registerReflectionHints(hints, httpEntityType);
      }
    }
  }

  protected void registerReturnTypeHints(ReflectionHints hints, MethodParameter returnTypeParameter) {
    if (AnnotatedElementUtils.hasAnnotation(returnTypeParameter.getContainingClass(), ResponseBody.class) ||
            returnTypeParameter.hasMethodAnnotation(ResponseBody.class)) {
      this.bindingRegistrar.registerReflectionHints(hints, returnTypeParameter.getGenericParameterType());
    }
    else if (HttpEntity.class.isAssignableFrom(returnTypeParameter.getParameterType())) {
      Type httpEntityType = getHttpEntityType(returnTypeParameter);
      if (httpEntityType != null) {
        this.bindingRegistrar.registerReflectionHints(hints, httpEntityType);
      }
    }
  }

  @Nullable
  private Type getHttpEntityType(MethodParameter parameter) {
    MethodParameter nestedParameter = parameter.nested();
    return (nestedParameter.getNestedParameterType() == nestedParameter.getParameterType() ?
            null : nestedParameter.getNestedParameterType());
  }

}
