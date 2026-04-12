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
 * A functional interface for invoking methods on objects.
 * <p>
 * This interface abstracts the invocation of a method, handling parameter unwrapping
 * and type conversions as necessary. It is typically used to cache reflective method
 * access for performance optimization.
 *
 * @author TODAY 2019-10-18 22:35
 */
@FunctionalInterface
public interface Invoker {

  /**
   * Invokes the underlying method on the specified object with the given arguments.
   * <p>
   * This method handles automatic unwrapping of parameters to match primitive
   * formal parameters and applies necessary method invocation conversions for
   * both primitive and reference types.
   *
   * @param obj the object on which the underlying method is invoked; may be {@code null} if the method is static
   * @param args the arguments used for the method call; may be {@code null} or empty if the method takes no parameters
   * @return the result of the method invocation, wrapped in an object if it is a primitive type;
   * {@code null} if the method return type is void
   * @throws NullPointerException if the specified object is {@code null} and the method is an instance method
   */
  @Nullable
  Object invoke(@Nullable Object obj, @Nullable Object @Nullable [] args);

}
