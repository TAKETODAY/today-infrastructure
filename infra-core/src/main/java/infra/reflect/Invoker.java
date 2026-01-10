/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.reflect;

import org.jspecify.annotations.Nullable;

/**
 * An invoker interface for method invocation.
 *
 * @author TODAY 2019-10-18 22:35
 */
@FunctionalInterface
public interface Invoker {

  /**
   * Invokes the underlying method represented by this {@code Invoker}
   * object, on the specified object with the specified parameters.
   * Individual parameters are automatically unwrapped to match
   * primitive formal parameters, and both primitive and reference
   * parameters are subject to method invocation conversions as
   * necessary.
   *
   * <p>If the underlying method is static, then the specified {@code obj}
   * argument is ignored. It may be null.
   *
   * <p>If the number of formal parameters required by the underlying method is
   * 0, the supplied {@code args} array may be of length 0 or null.
   *
   * <p>If the underlying method is static, the class that declared
   * the method is initialized if it has not already been initialized.
   *
   * <p>If the method completes normally, the value it returns is
   * returned to the caller of invoke; if the value has a primitive
   * type, it is first appropriately wrapped in an object. However,
   * if the value has the type of array of a primitive type, the
   * elements of the array are <i>not</i> wrapped in objects; in
   * other words, an array of primitive type is returned.  If the
   * underlying method return type is void, the invocation returns
   * null.
   *
   * @param obj the object the underlying method is invoked from
   * @param args the arguments used for the method call
   * @return the result of dispatching the method represented by
   * this object on {@code obj} with parameters
   * {@code args}
   * @throws NullPointerException if the specified object is null and the method is an instance method.
   * @throws ExceptionInInitializerError if the initialization provoked by this method fails.
   */
  @Nullable
  Object invoke(@Nullable Object obj, @Nullable Object[] args);

}
