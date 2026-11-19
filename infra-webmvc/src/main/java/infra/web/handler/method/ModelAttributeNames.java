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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.core.Conventions;
import infra.core.GenericTypeResolver;
import infra.core.MethodParameter;
import infra.util.Assert;
import infra.util.StringUtils;
import infra.web.bind.annotation.ModelAttribute;

/**
 * Naming conventions for model attributes supplied as handler method arguments or return values.
 *
 * @since 5.0
 */
public final class ModelAttributeNames {

  private ModelAttributeNames() {
  }

  /**
   * Derive the model attribute name for the given method parameter based on
   * a {@code @ModelAttribute} parameter annotation (if present) or falling
   * back on parameter type based conventions.
   *
   * @param parameter a descriptor for the method parameter
   * @return the derived name
   * @see Conventions#getVariableNameForParameter(MethodParameter)
   */
  public static String getNameForParameter(MethodParameter parameter) {
    ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
    String name = ann != null ? ann.value() : null;
    return StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter);
  }

  /**
   * Derive the model attribute name from the {@code @ModelAttribute} method annotation,
   * the declared return type if more specific than {@code Object}, or the actual return value type.
   *
   * @param returnValue the value returned from a method invocation
   * @param returnType a descriptor for the return type of the method
   * @return the derived name (never {@code null} or empty String)
   */
  public static String getNameForReturnValue(@Nullable Object returnValue, MethodParameter returnType) {
    ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
    if (ann != null && StringUtils.hasText(ann.value())) {
      return ann.value();
    }
    Method method = returnType.getMethod();
    Assert.state(method != null, "No handler method");
    Class<?> containingClass = returnType.getContainingClass();
    Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
    return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
  }

}
